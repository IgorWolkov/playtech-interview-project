package playtech.interview

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import playtech.interview.actors.MainActor
import playtech.interview.model.{Element, ElementId}
import playtech.interview.errors.AggregationServiceLikeBusyError
import playtech.interview.errors.AggregationServiceLikeUnexpectedError

import scala.concurrent.{ExecutionContextExecutor, Future}


object services {

  trait AggregationServiceLike {
    /*
      Changing the initial API is a bad manners rule,
      but I'd prefer to move `ec` and `timeout` configs outside
     */
    def addElement(e: Element)(implicit
                               ec: ExecutionContextExecutor,
                               timeout: Timeout): Future[Unit]

    def printTotal()(implicit
                     ec: ExecutionContextExecutor,
                     timeout: Timeout): Future[Map[ElementId, BigDecimal]]
  }

  object AggregationServiceLikeImpl extends AggregationServiceLike {

    private val system: ActorSystem = ActorSystem("AggregationServiceLike")
    private val mainActor: ActorRef = system.actorOf(MainActor.props())

    override def addElement(e: Element)(implicit
                                        ec: ExecutionContextExecutor,
                                        timeout: Timeout): Future[Unit] =
      (mainActor ? MainActor.Request.Add(e.elementId, e.value)) map { _ => {} }

    override def printTotal()(implicit
                              ec: ExecutionContextExecutor,
                              timeout: Timeout): Future[Map[ElementId, BigDecimal]] =
      (mainActor ? MainActor.Request.State) map {
        case MainActor.Response.StateResult(result) =>
          result

        case MainActor.Response.Busy(message) =>
          throw AggregationServiceLikeBusyError(message = s"Cannot execute request [$message]: service is busy, try again later")

        case MainActor.Response.UnexpectedMessage(message) =>
          throw AggregationServiceLikeBusyError(message = s"Cannot execute request [$message]: unexpected request")

        case unexpected =>
          throw AggregationServiceLikeBusyError(message = s"Unexpected result [$unexpected]")
      } recover { error =>
        throw AggregationServiceLikeUnexpectedError(message = s"Unexpected error [${error.getMessage}]", Option(error))
      }

  }

}
