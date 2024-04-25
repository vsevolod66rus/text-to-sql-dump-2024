package text2ql.api

import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder}
import sttp.tapir.Schema

import java.sql.Timestamp
import java.time.Instant
import scala.util.Try

case class AskResponsePayload(
    grid: GridWithDataRenderTypeResponseModel
)

sealed trait GridPropertyDataType

case class GridPropertyDataTypeString(dataType: String = "string")   extends GridPropertyDataType
case class GridPropertyDataTypeDouble(dataType: String = "double")   extends GridPropertyDataType
case class GridPropertyDataTypeLong(dataType: String = "long")       extends GridPropertyDataType
case class GridPropertyDataTypeDate(dataType: String = "datetime")   extends GridPropertyDataType
case class GridPropertyDataTypeBoolean(dataType: String = "boolean") extends GridPropertyDataType

sealed trait GridPropertyValue

case class GridPropertyValueString(value: String)   extends GridPropertyValue
case class GridPropertyValueLong(value: Long)       extends GridPropertyValue
case class GridPropertyValueDouble(value: Double)   extends GridPropertyValue
case class GridPropertyValueBoolean(value: Boolean) extends GridPropertyValue
case class GridPropertyValueInstant(value: Instant) extends GridPropertyValue

case class GridWithDataRenderTypeResponseModel(
    properties: List[GridPropertyItemModel],
    items: List[Map[String, GridPropertyValue]],
    total: Long
)

case class GridPropertyItemModel(
    key: String,
    title: String,
    dataType: GridPropertyDataType
)

case class AskRepoResponse(
    text: Option[String] = None,
    custom: Option[AskResponsePayload] = None,
    count: CountQueryDTO,
    query: Option[String] = None
)

object GridPropertyDataType {

  implicit val encodeGridPropertyDataType: Encoder[GridPropertyDataType] = Encoder.instance {
    case _ @GridPropertyDataTypeString(dataType)  => dataType.asJson
    case _ @GridPropertyDataTypeDouble(dataType)  => dataType.asJson
    case _ @GridPropertyDataTypeLong(dataType)    => dataType.asJson
    case _ @GridPropertyDataTypeDate(dataType)    => dataType.asJson
    case _ @GridPropertyDataTypeBoolean(dataType) => dataType.asJson
  }

  implicit val decodeGridPropertyDataType: Decoder[GridPropertyDataType] =
    Decoder[String].map {
      case "double"   => GridPropertyDataTypeDouble()
      case "long"     => GridPropertyDataTypeLong()
      case "datetime" => GridPropertyDataTypeDate()
      case "boolean"  => GridPropertyDataTypeBoolean()
      case _          => GridPropertyDataTypeString()
    }

  implicit val schema: Schema[GridPropertyDataType] = Schema.derived

  def fromType(attrType: String): GridPropertyDataType = attrType match {
    case "double"   => GridPropertyDataTypeDouble()
    case "long"     => GridPropertyDataTypeLong()
    case "boolean"  => GridPropertyDataTypeBoolean()
    case "datetime" => GridPropertyDataTypeDate()
    case _          => GridPropertyDataTypeString()
  }
}

object GridPropertyValue {

  implicit val encoderGridPropertyValue: Encoder[GridPropertyValue] = Encoder.instance {
    case _ @GridPropertyValueString(string)   => string.asJson
    case _ @GridPropertyValueDouble(double)   => double.asJson
    case _ @GridPropertyValueLong(long)       => long.asJson
    case _ @GridPropertyValueBoolean(boolean) => boolean.asJson
    case _ @GridPropertyValueInstant(instant) => instant.asJson
  }

  implicit val decoderGridPropertyValue: Decoder[GridPropertyValue] =
    Decoder[Instant]
      .map[GridPropertyValue](GridPropertyValueInstant)
      .or(Decoder[Boolean].map[GridPropertyValue](GridPropertyValueBoolean))
      .or(Decoder[Long].map[GridPropertyValue](GridPropertyValueLong))
      .or(Decoder[Double].map[GridPropertyValue](GridPropertyValueDouble))
      .or(Decoder[String].map[GridPropertyValue](GridPropertyValueString))

  implicit val schema: Schema[GridPropertyValue] = Schema.derived

  def fromValueAndType(value: String, attrType: String): GridPropertyValue = attrType match {
    case "double"   =>
      value.toDoubleOption.fold[GridPropertyValue](GridPropertyValueString(value))(GridPropertyValueDouble)
    case "long"     =>
      value.toLongOption.fold[GridPropertyValue](GridPropertyValueString(value))(GridPropertyValueLong)
    case "boolean"  =>
      { value match { case "t" => "True"; case "f" => "False"; case _ => value } }.toBooleanOption
        .fold[GridPropertyValue](GridPropertyValueString(value))(GridPropertyValueBoolean)
    case "datetime" =>
      Try(Timestamp.valueOf(value).toInstant).toOption
        .fold[GridPropertyValue](GridPropertyValueString(value))(GridPropertyValueInstant)
    case _          => GridPropertyValueString(value)
  }

}

object GridWithDataRenderTypeResponseModel {
  implicit val codec: Codec[GridWithDataRenderTypeResponseModel]   = deriveCodec
  implicit val schema: Schema[GridWithDataRenderTypeResponseModel] = Schema.derived
}

object GridPropertyItemModel {
  implicit val codec: Codec[GridPropertyItemModel]   = deriveCodec
  implicit val schema: Schema[GridPropertyItemModel] = Schema.derived
}

object AskResponsePayload {
  implicit val codec: Codec[AskResponsePayload]   = deriveCodec
  implicit val schema: Schema[AskResponsePayload] = Schema.derived
}
