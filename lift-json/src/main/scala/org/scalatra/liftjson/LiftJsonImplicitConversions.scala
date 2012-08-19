package org.scalatra
package liftjson

import util.conversion._
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import net.liftweb.json._
import json.JsonImplicitConversions

trait LiftJsonImplicitConversions extends JsonImplicitConversions[JValue] {

  implicit protected def jsonFormats: Formats

  implicit val jsonToBoolean: TypeConverter[JValue, Boolean] = safe(j => j.extractOpt[Boolean] getOrElse j.extract[String].toBoolean)

  implicit val jsonToFloat: TypeConverter[JValue, Float] = safe(j => j.extractOpt[Float] getOrElse j.extract[String].toFloat)

  implicit val jsonToDouble: TypeConverter[JValue, Double] = safe(j => j.extractOpt[Double] getOrElse j.extract[String].toDouble)

  implicit val jsonToByte: TypeConverter[JValue, Byte] = safe(j => j.extractOpt[Byte] getOrElse j.extract[String].toByte)

  implicit val jsonToShort: TypeConverter[JValue, Short] = safe(j => j.extractOpt[Short] getOrElse j.extract[String].toShort)

  implicit val jsonToInt: TypeConverter[JValue, Int] = safe(j => j.extractOpt[Int] getOrElse j.extract[String].toInt)

  implicit val jsonToLong: TypeConverter[JValue, Long] = safe(j => j.extractOpt[Long] getOrElse j.extract[String].toLong)

  implicit val jsonToSelf: TypeConverter[JValue, String] = safe(_.extract[String])

  implicit val jsonToBigInt: TypeConverter[JValue, BigInt] = safe(_ match {
    case JInt(bigint) => bigint
    case JString(v) => BigInt(v)
  })


  def jsonToDate(format: => String): TypeConverter[JValue, Date] = jsonToDateFormat(new SimpleDateFormat(format))

  def jsonToDateFormat(format: => DateFormat): TypeConverter[JValue, Date] =
    safeOption(_.extractOpt[String] map format.parse)

  def jsonToSeq[T:Manifest](elementConverter: TypeConverter[JValue, T], separator: String = ","): TypeConverter[JValue, Seq[T]] =
    safe(_.extract[List[T]])

}
