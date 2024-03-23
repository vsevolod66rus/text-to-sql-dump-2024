package text2ql.api

import enumeratum.{CirceEnum, Enum, EnumEntry, QuillEnum}
import io.circe.generic.extras.semiauto.deriveEnumerationCodec
import io.circe.{Codec, KeyDecoder, KeyEncoder}
import pureconfig.ConfigReader
import sttp.tapir.Codec.id
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{DecodeResult, Schema}
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait Domain extends EnumEntry

object Domain extends Enum[Domain] with TapirCodecEnumeratum with CirceEnum[Domain] with QuillEnum[Domain] {

  case object HR extends Domain

  override def values: IndexedSeq[Domain] = findValues

  implicit val codec: Codec[Domain] = deriveEnumerationCodec

  implicit val schema: Schema[Domain] = Schema.derived

  implicit val encoderMap: KeyEncoder[Domain] = KeyEncoder.instance(_.entryName)

  implicit val decoderMap: KeyDecoder[Domain] = KeyDecoder.instance(Domain.withNameOption)

  implicit val configReader: ConfigReader[Domain] = ConfigReader.stringConfigReader.map(Domain.withNameInsensitive)

  implicit val domainQueryParamCodec: sttp.tapir.Codec[List[String], Option[Domain], TextPlain] =
    id[List[String], TextPlain](TextPlain(), Schema.derived)
      .mapDecode {
        case List(e) => DecodeResult.Value(Option(Domain.withNameInsensitive(e)))
        case _       => DecodeResult.Value(None)
      }(d => List(d.map(_.entryName).getOrElse("")))

  implicit def domainsSchema[T: Schema]: Schema[Map[Domain, T]] = Schema.schemaForMap[Domain, T](_.entryName)

}
