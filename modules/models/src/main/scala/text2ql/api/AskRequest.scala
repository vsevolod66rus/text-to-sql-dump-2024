package text2ql.api

import enumeratum._
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

import java.util.UUID

case class ChatMessageRequestModel(
    sort: BaseSortModel,
    chat: AddChatMessageRequestModel,
    domain: Option[Domain] = None
)

case class BaseSortModel(
    orderBy: Option[String],
    direction: Option[SortDirection]
)

case class AddChatMessageRequestModel(message: String, requestId: UUID)

object ChatMessageRequestModel {
  implicit val codec: Codec[ChatMessageRequestModel]   = deriveCodec
  implicit val schema: Schema[ChatMessageRequestModel] = Schema.derived
}

object BaseSortModel {
  implicit val codec: Codec[BaseSortModel]   = deriveCodec
  implicit val schema: Schema[BaseSortModel] = Schema.derived
}

object AddChatMessageRequestModel {
  implicit val codec: Codec[AddChatMessageRequestModel]   = deriveCodec
  implicit val schema: Schema[AddChatMessageRequestModel] = Schema.derived
}

sealed trait SortDirection extends EnumEntry

object SortDirection extends Enum[SortDirection] with TapirCodecEnumeratum with CirceEnum[SortDirection] {
  case object asc  extends SortDirection
  case object desc extends SortDirection

  val values: IndexedSeq[SortDirection] = findValues
}
