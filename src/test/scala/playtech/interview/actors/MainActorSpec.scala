package playtech.interview.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import playtech.interview.model.ElementId

import scala.util.Random

class MainActorSpec extends TestKit(ActorSystem("MainActorSpec"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An MainActor actor" must {

    "respond with UnexpectedMessage when unexpected message is sent while accumulating" in {
      val mainActor = system.actorOf(MainActor.props(), generateActorName)

      val message = "hello world"
      mainActor ! message

      expectMsg(MainActor.Response.UnexpectedMessage(message))
    }

    "respond with AddResult when valid Add message is sent while accumulating" in {
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val mainActor = system.actorOf(MainActor.props(), generateActorName)

      val message = MainActor.Request.Add(id, value)
      mainActor ! message

      expectMsg(MainActor.Response.AddResult(id))
    }

    "respond with StateResult with an empty map when valid State message is sent" in {
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val mainActor = system.actorOf(MainActor.props(), generateActorName)

      mainActor ! MainActor.Request.State

      expectMsg(MainActor.Response.StateResult(Map.empty))
    }

    "respond with StateResult with a pair map when valid Add and State messages are sent" in {
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val mainActor = system.actorOf(MainActor.props(), generateActorName)

      val message = MainActor.Request.Add(id, value)
      mainActor ! message

      expectMsg(MainActor.Response.AddResult(id))

      mainActor ! MainActor.Request.State

      expectMsg(MainActor.Response.StateResult(Map(id -> value)))
    }

    "respond with StateResult with a bulk map when valid bulk Add and a State message are sent" in {

      /*
        Up to 1000 ElementActors with up to 10000 values per actor to sum
       */
      val idPerValues = ((1 to Random.between(2, 100)) map { _ =>
        ElementId(Random.nextInt()) -> ((1 to Random.between(2, 1000)) map { _ => BigDecimal(Random.nextDouble())})
      }).toMap

      val sumPerId = idPerValues map { case (id, values) => id -> values.sum }

      val mainActor = system.actorOf(MainActor.props(), generateActorName)

      idPerValues foreach { case (id, values) =>
        values foreach { value =>
          mainActor ! MainActor.Request.Add(id, value)
          expectMsg(MainActor.Response.AddResult(id))
        }
      }

      Thread.sleep(1000)

      mainActor ! MainActor.Request.State

      Thread.sleep(1000)

      expectMsg(MainActor.Response.StateResult(sumPerId))
    }

    /*
      Missing obvious cases:
         * Simultaneous MainActor.Request.State requests from different actors
         * Unexpected message while summation
         * ElementActor.Response:
          ** ElementActor.Response.UnsupportedEntityId
          ** ElementActor.Response.ParentActorSenderViolation
          ** ElementActor.Response.UnexpectedMessage
         * Recovery after crashes
         * etc
     */

  }

  private def generateActorName: String =
    s"Main-${Random.between(1, 100)}"

}
