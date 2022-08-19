package playtech.interview

object errors {

  trait AggregationServiceLikeDomainError extends Throwable {
    val message: String
    val cause: Option[Throwable]
    def getStackTraceString: String = getStackTrace.mkString(",\n")
    override def getMessage(): String = message
  }

  case class AggregationServiceLikeBusyError(message: String, cause: Option[Throwable] = None) extends AggregationServiceLikeDomainError
  case class AggregationServiceLikeBrokenApiError(message: String, cause: Option[Throwable] = None) extends AggregationServiceLikeDomainError
  case class AggregationServiceLikeUnexpectedError(message: String, cause: Option[Throwable] = None) extends AggregationServiceLikeDomainError

}
