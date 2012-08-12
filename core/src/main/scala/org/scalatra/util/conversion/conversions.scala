package org.scalatra
package util
package conversion

import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import scala.util.control.Exception.allCatch

/**
 * Support types and implicits for [[org.scalatra.common.conversions.TypeConverter]].
 */
trait TypeConverterSupport {

  implicit def safe[T](f: String => T): TypeConverter[T] = (s) => allCatch opt f(s)
  /**
   * Implicit convert a `(String) => Option[T]` function into a `TypeConverter[T]`
   */
  implicit def safeOption[T](f: String => Option[T]) = (s: String) => allCatch.withApply(_ => None)(f(s))
}


/**
 * Implicit TypeConverter values for value types and some factory method for
 * dates and seqs.
 */
trait DefaultImplicitConversions extends TypeConverterSupport {

  implicit val stringToBoolean: TypeConverter[Boolean] = safe(_.toBoolean)

  implicit val stringToFloat: TypeConverter[Float] = safe(_.toFloat)

  implicit val stringToDouble: TypeConverter[Double] = safe(_.toDouble)

  implicit val stringToByte: TypeConverter[Byte] = safe(_.toByte)

  implicit val stringToShort: TypeConverter[Short] = safe(_.toShort)

  implicit val stringToInt: TypeConverter[Int] = safe(_.toInt)

  implicit val stringToLong: TypeConverter[Long] = safe(_.toLong)

  implicit val stringToSelf: TypeConverter[String] = safe(s => s)

  def stringToDate(format: => String): TypeConverter[Date] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[Date] = safe(format.parse(_))

  def stringToSeq[T](elementConverter: TypeConverter[T], separator: String = ","): TypeConverter[Seq[T]] = safe(s => s.split(separator).flatMap(elementConverter.apply(_)))
}

object Conversions extends DefaultImplicitConversions {

  class ValConversion(source: String) {
    def as[T: TypeConverter]: Option[T] = implicitly[TypeConverter[T]].apply(source)
  }

  class DateConversion(source: String) {
    def asDate(format: String): Option[Date] = stringToDate(format).apply(source)
  }

  class SeqConversion(source: String) {

    def asSeq[T: TypeConverter](separator: String): Option[Seq[T]] = stringToSeq(implicitly[TypeConverter[T]], separator).apply(source)

  }

  implicit def stringToValTypeConversion(source: String) = new ValConversion(source)

  implicit def stringToDateConversion(source: String) = new DateConversion(source)

  implicit def stringToSeqConversion(source: String) = new SeqConversion(source)
}