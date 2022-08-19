package playtech.interview.actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.{Logging, LoggingAdapter}
import playtech.interview.model.ElementId

import scala.collection.mutable

object MainActor {

  sealed trait Request
  object Request {
    case class Add(id: ElementId, value: BigDecimal) extends Request
    case object State extends Request
  }

  sealed trait Response
  object Response {
    case class AddResult(id: ElementId) extends Response
    case class StateResult(result: Map[ElementId, BigDecimal]) extends Response
    case class UnexpectedMessage(message: Any) extends Response
    case class Busy(message: Any) extends Response
  }

  def props(): Props = Props(new MainActor())

}

class MainActor extends Actor {

  private val log: LoggingAdapter = Logging(context.system, this)
  // Would we have more than Int.MaxValue ids in the future?
  private val elementActors: mutable.Map[ElementId, ActorRef] = mutable.Map()
  private val state: mutable.Map[ElementId, BigDecimal] = mutable.Map()
  private val summationRequesters: mutable.Set[ActorRef] = mutable.Set()

  def receive: Receive = accumulating

  private def accumulating: Receive = {
    case message: MainActor.Request     => accumulatingProcessMainActorRequest(message)
    case message: ElementActor.Response => accumulatingProcessElementActorResponse(message)
    case unexpected                     => sender() ! processUnexpectedMessage(unexpected)
  }

  def summation: Receive = {
    case message: MainActor.Request     => summationProcessMainActorRequest(message)
    case message: ElementActor.Response => summationProcessElementActorResponse(message)
    case unexpected                     => sender() ! processUnexpectedMessage(unexpected)
  }

  private def accumulatingProcessMainActorRequest(request: MainActor.Request): Unit = {
    request match {
      case MainActor.Request.Add(id, value) =>
        getElementActor(id) ! ElementActor.Request.Add(id, value, sender())
        log.debug(s"Main Actor ${self.path} received [Add] request [$request] while accumulating")

      case MainActor.Request.State =>
        processStateRequest()
        log.debug(s"Main Actor ${self.path} received [State] request while accumulating")
    }
  }

  private def accumulatingProcessElementActorResponse(response: ElementActor.Response): Unit = {

    response match {
      case ElementActor.Response.AddResult(id, requester) =>
        requester ! MainActor.Response.AddResult(id)
        log.debug(s"MainActor ${self.path} received [AddResult] response [$response] from [${sender()}] while accumulating")

      case ElementActor.Response.StateResult(_, _) =>
        log.warning(s"MainActor ${self.path} received [AddResult] response [$response] from [${sender()}] while accumulating. The message will be ignored")

      case ElementActor.Response.UnsupportedEntityId(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [UnsupportedEntityId] response [$response] from [${sender()}] while accumulating. MainActor state is inconsistent or broken")

      case ElementActor.Response.ParentActorSenderViolation(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [ParentActorSenderViolation] response [$response] from [${sender()}] while accumulating. MainActor state is inconsistent or broken")

      case ElementActor.Response.UnexpectedMessage(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [ParentActorSenderViolation] response [$response] from [${sender()}] while accumulating. MainActor API is inconsistent or outdated")
    }

  }

  private def summationProcessMainActorRequest(request: MainActor.Request): Unit = {
    request match {
      case MainActor.Request.State =>
        /*
          We continue "collect" State request while collecting a sum, it doesn't affect the result
         */
        summationRequesters += sender()

      case _ =>
        /*
          We need to block all Add requests to ElementActors while MainActor calculates a sum.
         */
        sender() ! MainActor.Response.Busy(request)
        log.warning(s"Main Actor ${self.path} received request [$request] while summation")
    }
  }

  private def summationProcessElementActorResponse(response: ElementActor.Response): Unit = {
    response match {
      case ElementActor.Response.AddResult(id, requester) =>
        requester ! MainActor.Response.AddResult(id)
        log.debug(s"MainActor ${self.path} received [AddResult] response [$response] from [${sender()}] while summation.")

      case ElementActor.Response.StateResult(id, value) =>
        processSummation(id, value)
        log.debug(s"MainActor ${self.path} received [StateResult] response [$response] from [${sender()}] while summation.")

      case ElementActor.Response.UnsupportedEntityId(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [UnsupportedEntityId] response [$response] from [${sender()}] while summation. MainActor state is inconsistent or broken")

      case ElementActor.Response.ParentActorSenderViolation(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [ParentActorSenderViolation] response [$response] from [${sender()}] while summation. MainActor state is inconsistent or broken")

      case ElementActor.Response.UnexpectedMessage(_) =>
        // Should we kill MainActor?
        log.error(s"MainActor ${self.path} received [ParentActorSenderViolation] response [$response] from [${sender()}] while summation. MainActor API is inconsistent or outdated")
    }
  }

  private def processUnexpectedMessage(message: Any): MainActor.Response = {
    log.warning(s"MainActor ${self.path} received unexpected message [$message]")
    MainActor.Response.UnexpectedMessage(message)
  }

  def processStateRequest(): Unit = {
    if(elementActors.nonEmpty) {
      summationRequesters += sender()
      elementActors foreach { case (id, elementActor) =>
        elementActor ! ElementActor.Request.State(id)
      }
      context.become(summation)
    } else {
      sender() ! MainActor.Response.StateResult(Map.empty)
    }
  }

  def processSummation(id: ElementId, value: BigDecimal): Unit = {
    state.put(id, value)

    // For safety keys in `state` and `elementActors` could be compared
    if(state.size == elementActors.size) {
      summationRequesters foreach { requester =>
        requester ! MainActor.Response.StateResult(state.toMap)
      }

      elementActors.values foreach { elementActor =>
        elementActor ! PoisonPill
      }

      resetState()
    }

  }

  def resetState(): Unit = {
    elementActors.clear()
    summationRequesters.clear()
    state.clear()
    context.become(accumulating)
  }

  private def getElementActor(id: ElementId): ActorRef =
    elementActors.getOrElseUpdate(id, context.actorOf(ElementActor.props(id), s"ElementActor-${id.id}"))

}
