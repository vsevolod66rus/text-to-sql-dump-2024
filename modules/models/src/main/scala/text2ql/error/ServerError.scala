package text2ql.error

sealed trait ServerError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object ServerError {

  object TypeDBConnectionsLimitExceeded extends ServerError {
    override val message = "No database connections available"
  }

  final case class ServerErrorWithMessage(message: String) extends ServerError

  final case class TypeDBQueryException(cause: Throwable) extends Throwable
}
