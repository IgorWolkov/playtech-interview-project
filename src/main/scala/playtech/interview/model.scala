package playtech.interview

object model {

  case class ElementId(id: Int)
  case class Element(elementId: ElementId, value: BigDecimal)

}
