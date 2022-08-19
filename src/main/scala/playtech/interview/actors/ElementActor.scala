package playtech.interview.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.{Logging, LoggingAdapter}
import playtech.interview.actors.ElementActor.Response.UnsupportedEntityId
import playtech.interview.actors.ElementActor.{Request, Response}
import playtech.interview.model.ElementId

object ElementActor {

  sealed trait Request
  object Request {
    case class Add(id: ElementId, value: BigDecimal, requester: ActorRef) extends Request
    case class State(id: ElementId) extends Request
  }

  sealed trait Response
  object Response {
    case class AddResult(id: ElementId, requester: ActorRef) extends Response
    case class StateResult(id: ElementId, value: BigDecimal) extends Response
    case class UnsupportedEntityId(request: Request) extends Response
    case class ParentActorSenderViolation(request: Request) extends Response
    case class UnexpectedMessage(message: Any) extends Response
  }

  def props(id: ElementId): Props = Props(new ElementActor(id))

}

class ElementActor(id: ElementId) extends Actor {

  private val log: LoggingAdapter = Logging(context.system, this)
  // Should we keep all the history of `Request.Add`?
  private var state: BigDecimal = 0

  def receive: Receive = {
    case message: Request if sender() equals context.parent => sender() ! processMessage(message)
    case message: Request                                   => sender() ! processParentActorSenderViolation(message)
    case unexpected                                         => sender() ! processUnexpectedMessage(unexpected)
  }

  private def processMessage(request: Request): Response = {

    request match {
      case Request.Add(`id`, value, requester) =>
        log.debug(s"ElementActor [$id] ${self.path} received [Add] request [$request]")
        state += value
        Response.AddResult(id, requester)

      case request @ Request.Add(unsupported, _, _) =>
        log.warning(s"ElementActor [$id] ${self.path} received [Add] request [$request] with unsupported id [$unsupported]")
        UnsupportedEntityId(request)

      case Request.State(`id`) =>
        log.debug(s"ElementActor [$id] ${self.path} received [State] request [$request]")
        Response.StateResult(id, state)

      // Made intentionally to catch missing cases
      case request @ Request.State(unsupported) =>
        log.warning(s"ElementActor [$id] ${self.path} received [State] request [$request] with unsupported id [$unsupported]")
        UnsupportedEntityId(request)

      /*
       ```
        case request @ Request.Add(unsupported, _) =>
       ```
       and
       ```
        case request @ Request.State(unsupported) =>
       ```
       could be replaced with
       ```
        case request: Request => ...
       ```
       by moving `id: ElementId` to the `trait Request` level,
       but in this case it's impossible catch `match may not be exhaustive` warning
       what is crucial since we could miss an implementation of a new types of `Request`.

       Conclusion: copy-past strategy is justified.

       */
    }
  }

  private def processParentActorSenderViolation(request: Request): Response = {
    log.warning(s"ElementActor [$id] ${self.path} received a request [$request] not from a parent actor")
    Response.ParentActorSenderViolation(request)
  }

  private def processUnexpectedMessage(message: Any): Response = {
    log.warning(s"ElementActor [$id] ${self.path} received unexpected message [$message]")
    Response.UnexpectedMessage(message)
  }

}
