package org.scalatra
package liftjson

import util.conversion._
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import net.liftweb.json._
import json.JsonImplicitConversions

trait LiftJsonImplicitConversions extends JsonImplicitConversions[JValue] {

  implicit protected def jsonFormats: Formats

  implicit val jsonToBoolean: TypeConverter[JValue, Boolean] = safe(_.extract[Boolean])

  implicit val jsonToFloat: TypeConverter[JValue, Float] =  safe(_.extract[Float])

  implicit val jsonToDouble: TypeConverter[JValue, Double] = safe(_.extract[Double])

  implicit val jsonToByte: TypeConverter[JValue, Byte] = safe(_.extract[Byte])

  implicit val jsonToShort: TypeConverter[JValue, Short] = safe(_.extract[Short])

  implicit val jsonToInt: TypeConverter[JValue, Int] = safe(_.extract[Int])

  implicit val jsonToLong: TypeConverter[JValue, Long] = safe(_.extract[Long])

  implicit val jsonToSelf: TypeConverter[JValue, String] = safe(_.extract[String])

  def jsonToDate(format: => String): TypeConverter[JValue, Date] = jsonToDateFormat(new SimpleDateFormat(format))

  def jsonToDateFormat(format: => DateFormat): TypeConverter[JValue, Date] =
    safeOption(_.extractOpt[String] map format.parse)

  def jsonToSeq[T:Manifest](elementConverter: TypeConverter[JValue, T], separator: String = ","): TypeConverter[JValue, Seq[T]] =
    safe(_.extract[List[T]])

}
