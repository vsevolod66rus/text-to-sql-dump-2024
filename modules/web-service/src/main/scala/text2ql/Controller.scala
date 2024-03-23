package text2ql

trait Controller[F[_]] {
  def name: String = ""
  def endpoints: Seq[WSEndpoint[F]]
}
