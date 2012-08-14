package org.scalatra
package json

import org.scalatra.util.conversion._
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import scala.util.control.Exception._

trait JsonImplicitConversions[J] extends TypeConverterSupport {
  implicit def jsonToBoolean: TypeConverter[J, Boolean]
  
  implicit def jsonToFloat: TypeConverter[J, Float]

  implicit def jsonToDouble: TypeConverter[J, Double]

  implicit def jsonToByte: TypeConverter[J, Byte]

  implicit def jsonToShort: TypeConverter[J, Short]

  implicit def jsonToInt: TypeConverter[J, Int]

  implicit def jsonToLong: TypeConverter[J, Long]

  implicit def jsonToSelf: TypeConverter[J, String]

  def jsonToDate(format: => String): TypeConverter[J, Date]
  def jsonToDateFormat(format: => DateFormat): TypeConverter[J, Date]

  def jsonToSeq[T:Manifest](elementConverter: TypeConverter[J, T], separator: String = ","): TypeConverter[J, Seq[T]]

  import JsonConversions._
  implicit def jsonToValTypeConversion(source: J) = new JsonValConversion(source)

  implicit def jsonToDateConversion(source: J) = new JsonDateConversion(source, jsonToDate(_))

  implicit def jsonToSeqConversion(source: J) = new {
    def asSeq[T](separator: String)(implicit mf: Manifest[T], tc: TypeConverter[J, T]): Option[Seq[T]] =
         jsonToSeq[T](tc, separator).apply(source)
  }
}

object JsonConversions {

  class JsonValConversion[J](source: J) {
    private type JsonTypeConverter[T] = TypeConverter[J, T]
    def as[T: JsonTypeConverter]: Option[T] = implicitly[TypeConverter[J, T]].apply(source)
  }


  class JsonDateConversion[J](source: J, jsonToDate: String => TypeConverter[J, Date]) {
    def asDate(format: String): Option[Date] = jsonToDate(format).apply(source)
  }



}
