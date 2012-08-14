package org.scalatra.jackson

import org.scalatra.util.conversion._
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, MissingNode}
import collection.JavaConverters._
import org.scalatra.json.JsonImplicitConversions

trait JacksonImplicitConversions extends JsonImplicitConversions[JsonNode] {

  implicit val jsonToBoolean: TypeConverter[JsonNode, Boolean] = safeOption {  json =>
      Option(json) filter (_.isBoolean) map (_.asBoolean())
    }

  implicit val jsonToFloat: TypeConverter[JsonNode, Float] =  safeOption { json =>
      Option(json) filter (_.isFloatingPointNumber) map (_.asText().toFloat)
    }

  implicit val jsonToDouble: TypeConverter[JsonNode, Double] = safeOption { json =>
      Option(json) filter (_.isDouble) map (_.asDouble())
    }

  implicit val jsonToByte: TypeConverter[JsonNode, Byte] = safeOption { json =>
      Option(json) filter (j => j.isInt && j.asInt() >= Byte.MinValue && j.asInt() <= Byte.MaxValue) map (_.asInt().toByte)
    }

  implicit val jsonToShort: TypeConverter[JsonNode, Short] = safeOption { json =>
      Option(json) filter (j => j.isInt && j.asInt() <= Short.MaxValue && j.asInt() > Short.MinValue) map (_.asInt().toShort)
    }

  implicit val jsonToInt: TypeConverter[JsonNode, Int] = safeOption { json =>
      Option(json) filter (_.isInt) map (_.asInt())
    }

  implicit val jsonToLong: TypeConverter[JsonNode, Long] = safeOption { json =>
    Option(json) filter (j => j.isLong || j.isInt || j.isBigInteger) map (_.asLong())
  }

  implicit val jsonToSelf: TypeConverter[JsonNode, String] = safeOption { json =>
    Option(json) filter (_.isTextual) map (_.asText())
  }

  def jsonToDate(format: => String): TypeConverter[JsonNode, Date] = jsonToDateFormat(new SimpleDateFormat(format))

  def jsonToDateFormat(format: => DateFormat): TypeConverter[JsonNode, Date] = safeOption { json =>
      Option(json) filter (_.isTextual) map (j => format.parse(j.asText()))
    }

  def jsonToSeq[T:Manifest](elementConverter: TypeConverter[JsonNode, T], separator: String = ","): TypeConverter[JsonNode, Seq[T]] =
    safeOption {  json =>
      Option(json) filter (_.isArray) map (j => j.asInstanceOf[ArrayNode].asScala.map(elementConverter(_).getOrElse(null.asInstanceOf[T])).toSeq)
    }


}
