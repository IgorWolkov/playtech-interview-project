package playtech.interview

import akka.actor.ActorSystem
import akka.util.Timeout
import playtech.interview.model.{Element, ElementId}
import playtech.interview.services.{AggregationServiceLike, AggregationServiceLikeImpl}

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.DurationInt

object Main extends App {

  implicit val ex: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val timeout: Timeout = Timeout(1.second)

  val service: AggregationServiceLike = AggregationServiceLikeImpl

  service.addElement(Element(ElementId(0), BigDecimal(0)))
  service.addElement(Element(ElementId(1), BigDecimal(0)))
  service.addElement(Element(ElementId(1), BigDecimal(10)))
  service.addElement(Element(ElementId(1), BigDecimal(20)))
  service.addElement(Element(ElementId(1), BigDecimal(30)))
  service.addElement(Element(ElementId(1), BigDecimal(40)))

  service.addElement(Element(ElementId(2), BigDecimal(100)))
  service.addElement(Element(ElementId(2), BigDecimal(200)))
  service.addElement(Element(ElementId(2), BigDecimal(300)))
  service.addElement(Element(ElementId(2), BigDecimal(400)))

  println(Await.result(service.printTotal(), 5.seconds)) // should print Map(0->0, 1->100, 2-> 1000)


  service.addElement(Element(ElementId(3), BigDecimal(100)))
  service.addElement(Element(ElementId(3), BigDecimal(200)))
  service.addElement(Element(ElementId(3), BigDecimal(300)))

  service.addElement(Element(ElementId(4), BigDecimal(0)))

  println(Await.result(service.printTotal(), 5.seconds)) // should print Map(3->600, 4->0)

  system.terminate()
}
