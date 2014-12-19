package org.scalatra
package util
package conversion

import java.util.Date
import java.text.{ DateFormat, SimpleDateFormat }
import scala.util.control.Exception.allCatch
import scala._

/**
 * Support types and implicits for [[org.scalatra.util.conversion.TypeConverter]].
 */
trait TypeConverterSupport {

  implicit def safe[S, T](f: S => T): TypeConverter[S, T] = new TypeConverter[S, T] {
    def apply(s: S): Option[T] = allCatch opt f(s)
  }
  /**
   * Implicit convert a `(String) => Option[T]` function into a `TypeConverter[T]`
   */
  implicit def safeOption[S, T](f: S => Option[T]): TypeConverter[S, T] = new TypeConverter[S, T] {
    def apply(v1: S): Option[T] = allCatch.withApply(_ => None)(f(v1))
  }

}

object TypeConverterSupport extends TypeConverterSupport

trait LowestPriorityImplicitConversions extends TypeConverterSupport {
  implicit def lowestPriorityAny2T[T: Manifest]: TypeConverter[Any, T] = safe {
    case a if manifest[T].erasure.isAssignableFrom(a.getClass) => a.asInstanceOf[T]
  }
}

trait LowPriorityImplicitConversions extends LowestPriorityImplicitConversions {

  implicit val anyToBoolean: TypeConverter[Any, Boolean] = safe {
    case b: Boolean => b
    case "ON" | "TRUE" | "OK" | "1" | "CHECKED" | "YES" | "ENABLE" | "ENABLED" => true
    case n: Number => n != 0
    case _ => false
  }

  implicit val anyToFloat: TypeConverter[Any, Float] = safe {
    case i: Byte => i.toFloat
    case i: Short => i.toFloat
    case i: Int => i.toFloat
    case i: Long => i.toFloat
    case i: Double => i.toFloat
    case i: Float => i
    case i: String => i.toFloat
  }

  implicit val anyToDouble: TypeConverter[Any, Double] = safe {
    case i: Byte => i.toDouble
    case i: Short => i.toDouble
    case i: Int => i.toDouble
    case i: Long => i.toDouble
    case i: Double => i
    case i: Float => i.toDouble
    case i: String => i.toDouble
  }

  implicit val anyToByte: TypeConverter[Any, Byte] = safe {
    case i: Byte => i
    case i: Short => i.toByte
    case i: Int => i.toByte
    case i: Long => i.toByte
    case i: Double => i.toByte
    case i: Float => i.toByte
    case i: String => i.toByte
  }

  implicit val anyToShort: TypeConverter[Any, Short] = safe {
    case i: Byte => i.toShort
    case i: Short => i
    case i: Int => i.toShort
    case i: Long => i.toShort
    case i: Double => i.toShort
    case i: Float => i.toShort
    case i: String => i.toShort
  }

  implicit val anyToInt: TypeConverter[Any, Int] = safe {
    case i: Byte => i.toInt
    case i: Short => i.toInt
    case i: Int => i
    case i: Long => i.toInt
    case i: Double => i.toInt
    case i: Float => i.toInt
    case i: String => i.toInt
  }

  implicit val anyToLong: TypeConverter[Any, Long] = safe {
    case i: Byte => i.toLong
    case i: Short => i.toLong
    case i: Int => i.toLong
    case i: Long => i
    case i: Double => i.toLong
    case i: Float => i.toLong
    case i: String => i.toLong
  }

  implicit val anyToString: TypeConverter[Any, String] = safe(_.toString)

}

trait BigDecimalImplicitConversions { self: DefaultImplicitConversions =>
  implicit val stringToBigDecimal: TypeConverter[String, BigDecimal] = safe(BigDecimal(_))
  implicit val stringToSeqBigDecimal: TypeConverter[String, Seq[BigDecimal]] = stringToSeq(stringToBigDecimal)
}

/**
 * Implicit TypeConverter values for value types and some factory method for
 * dates and seqs.
 */
trait DefaultImplicitConversions extends LowPriorityImplicitConversions {

  implicit val stringToBoolean: TypeConverter[String, Boolean] = safe { s =>
    s.toUpperCase match {
      case "ON" | "TRUE" | "OK" | "1" | "CHECKED" | "YES" | "ENABLE" | "ENABLED" => true
      case _ => false
    }
  }

  implicit val stringToFloat: TypeConverter[String, Float] = safe(_.toFloat)

  implicit val stringToDouble: TypeConverter[String, Double] = safe(_.toDouble)

  implicit val stringToByte: TypeConverter[String, Byte] = safe(_.toByte)

  implicit val stringToShort: TypeConverter[String, Short] = safe(_.toShort)

  implicit val stringToInt: TypeConverter[String, Int] = safe(_.toInt)

  implicit val stringToLong: TypeConverter[String, Long] = safe(_.toLong)

  implicit val stringToSelf: TypeConverter[String, String] = safe(identity)

  def stringToDate(format: => String): TypeConverter[String, Date] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[String, Date] = safe(format.parse(_))

  implicit def defaultStringToSeq[T](implicit elementConverter: TypeConverter[String, T], mf: Manifest[T]): TypeConverter[String, Seq[T]] =
    stringToSeq[T](elementConverter)

  def stringToSeq[T: Manifest](elementConverter: TypeConverter[String, T], separator: String = ","): TypeConverter[String, Seq[T]] =
    safe(s => s.split(separator).toSeq.flatMap(e => elementConverter(e.trim)))

  implicit def seqHead[T](implicit elementConverter: TypeConverter[String, T], mf: Manifest[T]): TypeConverter[Seq[String], T] =
    safeOption(_.headOption.flatMap(elementConverter(_)))

  implicit def seqToSeq[T](implicit elementConverter: TypeConverter[String, T], mf: Manifest[T]): TypeConverter[Seq[String], Seq[T]] =
    safe(_.flatMap(elementConverter(_)))

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

    def asSeq[T](separator: String)(implicit mf: Manifest[T], tc: TypeConverter[String, T]): Option[Seq[T]] =
      stringToSeq[T](tc, separator).apply(source)

  }

  implicit def stringToValTypeConversion(source: String) = new ValConversion(source)

  implicit def stringToDateConversion(source: String) = new DateConversion(source)

  implicit def stringToSeqConversion(source: String) = new SeqConversion(source)
}
