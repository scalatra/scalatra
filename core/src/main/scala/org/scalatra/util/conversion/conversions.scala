package org.scalatra
package util
package conversion

import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import scala.util.control.Exception.allCatch

/**
 * Support types and implicits for [[org.scalatra.common.conversions.TypeConverter]].
 */
trait TypeConverterSupport[S] {

  implicit def safe[T](f: S => T): TypeConverter[S, T] = (s) => allCatch opt f(s)
  /**
   * Implicit convert a `(String) => Option[T]` function into a `TypeConverter[T]`
   */
  implicit def safeOption[T](f: S => Option[T]) = (s: S) => allCatch.withApply(_ => None)(f(s))
}


/**
 * Implicit TypeConverter values for value types and some factory method for
 * dates and seqs.
 */
trait DefaultImplicitConversions extends TypeConverterSupport[String] {

  implicit val stringToBoolean: TypeConverter[String, Boolean] = safe(_.toBoolean)

  implicit val stringToFloat: TypeConverter[String, Float] = safe(_.toFloat)

  implicit val stringToDouble: TypeConverter[String, Double] = safe(_.toDouble)

  implicit val stringToByte: TypeConverter[String, Byte] = safe(_.toByte)

  implicit val stringToShort: TypeConverter[String, Short] = safe(_.toShort)

  implicit val stringToInt: TypeConverter[String, Int] = safe(_.toInt)

  implicit val stringToLong: TypeConverter[String, Long] = safe(_.toLong)

  implicit val stringToSelf: TypeConverter[String, String] = safe(s => s)



  def stringToDate(format: => String): TypeConverter[String, Date] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[String, Date] = safe(format.parse(_))

  def stringToSeq[T](elementConverter: TypeConverter[String, T], separator: String = ","): TypeConverter[String, Seq[T]] = safe(s => s.split(separator).flatMap(elementConverter.apply(_)))
}

object Conversions extends DefaultImplicitConversions {

  private type StringTypeConverter[T] = TypeConverter[String, T]
  class ValConversion(source: String) {
    def as[T: StringTypeConverter]: Option[T] = implicitly[TypeConverter[String, T]].apply(source)
  }

  class DateConversion(source: String) {
    def asDate(format: String): Option[Date] = stringToDate(format).apply(source)
  }

  class SeqConversion(source: String) {

    def asSeq[T](separator: String)(implicit tc: TypeConverter[String, T]): Option[Seq[T]] =
      stringToSeq[T](tc, separator).apply(source)

  }

  implicit def stringToValTypeConversion(source: String) = new ValConversion(source)

  implicit def stringToDateConversion(source: String) = new DateConversion(source)

  implicit def stringToSeqConversion(source: String) = new SeqConversion(source)
}

case class ValueHolder[T: Manifest](value: Option[T])
trait ValueHolderImplicits  {
  private[scalatra] implicit def vh[T: Manifest](t: T): ValueHolder[T] = ValueHolder(Option(t))
  private[scalatra] implicit def ovh[T: Manifest](t: Option[T]): ValueHolder[T] = ValueHolder(t)
}

trait ValueHolderImplicitConversions extends TypeConverterSupport[ValueHolder[_]] {

}