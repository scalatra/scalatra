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

  implicit def safe[S, T](f: S => T): TypeConverter[S, T] = (s) => allCatch opt f(s)
  /**
   * Implicit convert a `(String) => Option[T]` function into a `TypeConverter[T]`
   */
  implicit def safeOption[S, T](f: S => Option[T]) = (s: S) => allCatch.withApply(_ => None)(f(s))

  implicit def any2converted[T:Manifest](a: T) = new Converted[T] { val value: T = a }
}

object TypeConverterSupport extends TypeConverterSupport

/**
 * Implicit TypeConverter values for value types and some factory method for
 * dates and seqs.
 */
trait DefaultImplicitConversions extends TypeConverterSupport {

  implicit val stringToBoolean: TypeConverter[String, Boolean] = safe { s => s.toUpperCase match {
    case "ON" | "TRUE" | "OK" | "1" | "CHECKED" => true
    case _ => false
  } }

  implicit val stringToFloat: TypeConverter[String, Float] = safe(_.toFloat)

  implicit val stringToDouble: TypeConverter[String, Double] = safe(_.toDouble)

  implicit val stringToByte: TypeConverter[String, Byte] = safe(_.toByte)

  implicit val stringToShort: TypeConverter[String, Short] = safe(_.toShort)

  implicit val stringToInt: TypeConverter[String, Int] = safe(_.toInt)

  implicit val stringToLong: TypeConverter[String, Long] = safe(_.toLong)

  implicit val stringToSelf: TypeConverter[String, String] = safe(identity)




  def stringToDate(format: => String): TypeConverter[String, Date] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[String, Date] = safe(format.parse(_))

  def stringToSeq[T](elementConverter: TypeConverter[String, T], separator: String = ","): TypeConverter[String, Seq[T]] =
    safe(s => s.split(separator).toSeq.flatMap(e => elementConverter.apply(e)))
}

/**
 * Implicit TypeConverter values for value types and some factory method for
 * dates and seqs.
 */
trait ConvertedImplicitConversions extends TypeConverterSupport {

  implicit val stringToBoolean: TypeConverter[String, ConvertedBoolean] = safe { s => s.toUpperCase match {
    case "ON" | "TRUE" | "OK" | "1" | "CHECKED" => ConvertedBoolean(true)
    case _ => ConvertedBoolean(false)
  } }

  implicit val stringToFloat: TypeConverter[String, ConvertedFloat] = safe((s: String) => ConvertedFloat(s.toFloat))

  implicit val stringToDouble: TypeConverter[String, ConvertedDouble] = safe(j => ConvertedDouble(j.toDouble))

  implicit val stringToByte: TypeConverter[String, ConvertedByte] = safe(s => ConvertedByte(s.toByte))

  implicit val stringToShort: TypeConverter[String, ConvertedShort] = safe(s => ConvertedShort(s.toShort))

  implicit val stringToInt: TypeConverter[String, ConvertedInt] = safe(s => ConvertedInt(s.toInt))

  implicit val stringToLong: TypeConverter[String, ConvertedLong] = safe(s => ConvertedLong(s.toLong))

  implicit val stringToSelf: TypeConverter[String, ConvertedString] = safe(ConvertedString.apply)




  def stringToDate(format: => String): TypeConverter[String, ConvertedDate] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[String, ConvertedDate] = safe(s => ConvertedDate(format.parse(s)))

  def stringToSeq[T:Manifest](elementConverter: TypeConverter[String, Converted[T]], separator: String = ","): TypeConverter[String, Converted[Seq[T]]] =
    safe(s => s.split(separator).toSeq.flatMap(e => elementConverter.apply(e).map(_.value)): Converted[Seq[T]])
}

abstract class Converted[T : Manifest] {
  def value: T
}
case class ConvertedString(value: String) extends Converted[String]
case class ConvertedBoolean(value: Boolean) extends Converted[Boolean]
case class ConvertedFloat(value: Float) extends Converted[Float]
case class ConvertedDouble(value: Double) extends Converted[Double]
case class ConvertedByte(value: Byte) extends Converted[Byte]
case class ConvertedShort(value: Short) extends Converted[Short]
case class ConvertedInt(value: Int) extends Converted[Int]
case class ConvertedLong(value: Long) extends Converted[Long]
case class ConvertedDate(value: Date) extends Converted[Date]
case class ConvertedSeq[T:Manifest](value: Seq[T]) extends Converted[Seq[T]]


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

case class ValueHolder[T: Manifest](value: Option[T]) {
  def apply(): Option[T] = value
}
trait ValueHolderImplicits  {
  private[scalatra] implicit def vh[T: Manifest](t: T): ValueHolder[T] = ValueHolder(Option(t))
  private[scalatra] implicit def ovh[T: Manifest](t: Option[T]): ValueHolder[T] = ValueHolder(t)
}
//
//trait ValueHolderImplicitConversions extends TypeConverterSupport[ValueHolder[_]] {
//
//}