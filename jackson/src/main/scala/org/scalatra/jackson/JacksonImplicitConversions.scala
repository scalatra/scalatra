package org.scalatra.jackson

import org.scalatra.util.conversion._
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{DecimalNode, ArrayNode, MissingNode}
import collection.JavaConverters._
import org.scalatra.json.JsonImplicitConversions

trait JacksonImplicitConversions extends JsonImplicitConversions[JsonNode] {

  implicit val jsonToBoolean: TypeConverter[JsonNode, Boolean] = safeOption {  json =>
    Option(json) filter (_.isBoolean) map (_.asBoolean()) orElse fromTextNode(json, _.toBoolean)
  }

  implicit val jsonToFloat: TypeConverter[JsonNode, Float] =  safeOption { json =>
    val fromNumber = Option(json) filter (_.isFloatingPointNumber) map (_.asText().toFloat)
    fromNumber orElse fromTextNode(json, _.toFloat)
  }

  implicit val jsonToDouble: TypeConverter[JsonNode, Double] = safeOption { json =>
    val fromNumber = Option(json) filter (_.isDouble) map (_.asDouble())
    fromNumber orElse fromTextNode(json, _.toDouble)
  }

  implicit val jsonToBigDecimal: TypeConverter[JsonNode, BigDecimal] = safeOption { json =>
    val fromNumber = Option(json) filter (s => s.isBigDecimal) map (_.numberValue().asInstanceOf[BigDecimal])
    fromNumber orElse fromTextNode(json, BigDecimal(_:String))
  }

  implicit val jsonToByte: TypeConverter[JsonNode, Byte] = safeOption { json =>
    val fromNumber = Option(json) filter (j => j.isInt && j.asInt() >= Byte.MinValue && j.asInt() <= Byte.MaxValue) map (_.asInt().toByte)
    fromNumber orElse fromTextNode(json, _.toByte)
  }

  private def fromTextNode[T](json: JsonNode, convert: String => T) =
    Option(json) filter (_.isTextual) map (s => convert(s.asText()))

  implicit val jsonToShort: TypeConverter[JsonNode, Short] = safeOption { json =>
    val fromNumber = Option(json) filter (j => j.isInt && j.asInt() <= Short.MaxValue && j.asInt() > Short.MinValue) map (_.asInt().toShort)
//    val fromString = Option(json) filter (j => j.isTextual)
    fromNumber orElse fromTextNode(json, _.toShort)
  }

  implicit val jsonToInt: TypeConverter[JsonNode, Int] = safeOption { json =>
    Option(json) filter (_.isInt) map (_.asInt()) orElse fromTextNode(json, _.toInt)
  }


  implicit val jsonToBigInt: TypeConverter[JsonNode, BigInt] = safeOption { json =>
    Option(json) filter (_.isBigInteger) map (i => BigInt(i.textValue())) orElse fromTextNode(json, BigInt(_:String))
  }

  implicit val jsonToLong: TypeConverter[JsonNode, Long] = safeOption { json =>
    Option(json) filter (j => j.isLong || j.isInt || j.isBigInteger) map (_.asLong()) orElse fromTextNode(json, _.toLong)
  }

  implicit val jsonToSelf: TypeConverter[JsonNode, String] = safeOption { json =>
    Option(json) filter (_.isTextual) map (_.asText())
  }

  implicit val jsonToSeqBigDecimal: TypeConverter[JsonNode, Seq[BigDecimal]] = jsonToSeq(jsonToBigDecimal)

  def jsonToDate(format: => String): TypeConverter[JsonNode, Date] = jsonToDateFormat(new SimpleDateFormat(format))

  def jsonToDateFormat(format: => DateFormat): TypeConverter[JsonNode, Date] = safeOption { json =>
      Option(json) filter (_.isTextual) map (j => format.parse(j.asText()))
    }

  def jsonToSeq[T:Manifest](elementConverter: TypeConverter[JsonNode, T], separator: String = ","): TypeConverter[JsonNode, Seq[T]] =
    safeOption {  json =>
      Option(json) filter (_.isArray) map (j => j.asInstanceOf[ArrayNode].asScala.map(elementConverter(_).getOrElse(null.asInstanceOf[T])).toSeq)
    }


}
