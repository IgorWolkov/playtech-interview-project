## Playtech interview project

### Task

1. Implement AggregationServiceLike. It should create the main actor and then create separate child actors per ElementId.
   Child actors keep a sum of values and provide it to the parent actor by request.
2. After run printTotal, the main actor ask all their child actors about the sum of values.
3. After that the main actor kill all their children.

```scala
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class ElementId(id: Int)
case class Element(elementId: ElementId, value: BigDecimal)

trait AggregationServiceLike {
  def addElement(e: Element): Future[Unit]
  def printTotal(): Future[Map[ElementId, BigDecimal]]
}



object Main extends App {

  implicit val ex = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem("test")

  val service: AggregationServiceLike = ???

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

}
```