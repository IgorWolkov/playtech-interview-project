package playtech.interview.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import playtech.interview.actors.ElementActor.Request
import playtech.interview.actors.ElementActor.Response
import playtech.interview.model.ElementId

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

class ElementActorSpec extends TestKit(ActorSystem("ElementActorSpec"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An ElementActor actor" must {

    "respond with UnexpectedMessage when unexpected message is sent" in {
      val id = ElementId(Random.nextInt())
      val elementActor = system.actorOf(ElementActor.props(id))

      val message = "hello world"
      elementActor ! message

      expectMsg(Response.UnexpectedMessage(message))
    }

    /*
      We need to block all Add requests to ElementActors while MainActor calculates a sum.
      Since any actor in a system could send Add message, it is needed to block access to
      ElementActors outside of parent MainActor
     */
    "respond with ParentActorSenderViolation when Add request is sent by non-parent actor" in {
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val elementActor = system.actorOf(ElementActor.props(id))

      val message = Request.Add(id, value, self)
      elementActor ! message

      expectMsg(Response.ParentActorSenderViolation(message))
    }

    /*
      Let's block any access for an outside actors for simplicity
     */
    "respond with ParentActorSenderViolation when State request is sent by non-parent actor" in {
      val id = ElementId(Random.nextInt())

      val elementActor = system.actorOf(ElementActor.props(id))

      val message = Request.State(id)
      elementActor ! message

      expectMsg(Response.ParentActorSenderViolation(message))
    }

    /*
      MainActor state could be broken
     */
    "respond with UnsupportedEntityId when Add request is sent for a different ElementId an ElementActor is created for" in {
      val timeout: FiniteDuration = 1.second
      val id = ElementId(Random.nextInt())
      val unsupportedId = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val (parentActor, elementActor) = prepareActors(id, timeout)

      val message = Request.Add(unsupportedId, value, self)
      elementActor.tell(message, parentActor)

      expectMsg(Response.UnsupportedEntityId(message))
    }

    "respond with UnsupportedEntityId when State request is sent for a different ElementId an ElementActor is created for" in {
      val timeout: FiniteDuration = 1.second
      val id = ElementId(Random.nextInt())
      val unsupportedId = ElementId(Random.nextInt())

      val (parentActor, elementActor) = prepareActors(id, timeout)

      val message = Request.State(unsupportedId)
      elementActor.tell(message, parentActor)

      expectMsg(Response.UnsupportedEntityId(message))
    }

    "respond with AddResult when Add request is sent correctly" in {
      val timeout: FiniteDuration = 1.second
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(Random.nextDouble())

      val (parentActor, elementActor) = prepareActors(id, timeout)

      elementActor.tell(Request.Add(id, value, self), parentActor)

      expectMsg(Response.AddResult(id, self))
    }

    "respond with StateResult and a value 0 when State request is sent as a first message" in {
      val timeout: FiniteDuration = 1.second
      val id = ElementId(Random.nextInt())
      val value = BigDecimal(0)

      val (parentActor, elementActor) = prepareActors(id, timeout)

      elementActor.tell(Request.State(id), parentActor)

      expectMsg(Response.StateResult(id, value))
    }

    "respond with StateResult and a valid sum for a bulk Add request and a final State request" in {
      val timeout: FiniteDuration = 1.second
      val id = ElementId(Random.nextInt())
      val values = (1 to Random.between(2, 1000)) map { _ => BigDecimal(Random.nextDouble())}

      val (parentActor, elementActor) = prepareActors(id, timeout)

      values foreach { value =>
        elementActor.tell(Request.Add(id, value, self), parentActor)
        expectMsg(Response.AddResult(id, self))
      }

      elementActor.tell(Request.State(id), parentActor)

      expectMsg(Response.StateResult(id, values.sum))
    }

  }

  private def prepareActors(id: ElementId, timeout: FiniteDuration): (ActorRef, ActorRef) = {

    val parentActorName = generateActorName
    val parentActor = system.actorOf(ParentActor.props(self, id), parentActorName)
    // Waiting for parent actor is started
    Thread.sleep(100)
    val elementActor: ActorRef = Await.result(system.actorSelection(s"/user/$parentActorName/ElementActor-${id.id}").resolveOne(timeout), timeout)

    (parentActor, elementActor)
  }

  private object ParentActor {
    def props(testContextActor: ActorRef, id: ElementId): Props = Props(new ParentActor(testContextActor, id))
  }
  /*
    Need "proxy" ParentActor to overcome ParentActorSenderViolation errors
   */
  private class ParentActor(testContextActor: ActorRef, id: ElementId) extends Actor {
    context.actorOf(ElementActor.props(id), s"ElementActor-${id.id}")
    override def receive: Receive = {
      // Forward the message to the test context actor
      message => testContextActor.tell(message, sender())
    }
  }

  private def generateActorName: String =
    s"Main-${Random.between(1, 100)}"

}
