package text2ql.error

sealed trait ServerError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object ServerError {
  final case class ServerErrorWithMessage(message: String) extends ServerError
}
