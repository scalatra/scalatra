package org.scalatra
package util
package conversion

import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}
import scala.util.control.Exception.allCatch
import scala._

/**
 * Support types and implicits for [[org.scalatra.common.conversions.TypeConverter]].
 */
trait TypeConverterSupport {

  implicit def safe[S, T](f: S => T): TypeConverter[S, T] = (s) => allCatch opt f(s)
  /**
   * Implicit convert a `(String) => Option[T]` function into a `TypeConverter[T]`
   */
  implicit def safeOption[S, T](f: S => Option[T]) = (s: S) => allCatch.withApply(_ => None)(f(s))

}

object TypeConverterSupport extends TypeConverterSupport

trait BigDecimalImplicitConversions extends TypeConverterSupport { self: DefaultImplicitConversions =>
  implicit val stringToBigDecimal: TypeConverter[String, BigDecimal] = safe(BigDecimal(_))
  implicit val stringToSeqBigDecimal: TypeConverter[String, Seq[BigDecimal]] = stringToSeq(stringToBigDecimal)
  implicit val stringSeqToBigDecimal: TypeConverter[Seq[String], BigDecimal] = seqHead(stringToBigDecimal)
  implicit val stringSeqToSeqBigDecimal: TypeConverter[Seq[String], Seq[BigDecimal]] = seqToSeq(stringToBigDecimal)
}

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

  implicit val stringToSeqBoolean: TypeConverter[String, Seq[Boolean]] = stringToSeq(stringToBoolean)

  implicit val stringToSeqFloat: TypeConverter[String, Seq[Float]] = stringToSeq(stringToFloat)

  implicit val stringToSeqDouble: TypeConverter[String, Seq[Double]] = stringToSeq(stringToDouble)

  implicit val stringToSeqByte: TypeConverter[String, Seq[Byte]] = stringToSeq(stringToByte)

  implicit val stringToSeqShort: TypeConverter[String, Seq[Short]] = stringToSeq(stringToShort)

  implicit val stringToSeqInt: TypeConverter[String, Seq[Int]] = stringToSeq(stringToInt)

  implicit val stringToSeqLong: TypeConverter[String, Seq[Long]] = stringToSeq(stringToLong)

  implicit val stringToSeqString: TypeConverter[String, Seq[String]] = stringToSeq(stringToSelf)

  def stringToDate(format: => String): TypeConverter[String, Date] = stringToDateFormat(new SimpleDateFormat(format))

  def stringToDateFormat(format: => DateFormat): TypeConverter[String, Date] = safe(format.parse(_))

  def stringToSeq[T:Manifest](elementConverter: TypeConverter[String, T], separator: String = ","): TypeConverter[String, Seq[T]] =
    safe(s => s.split(separator).toSeq.flatMap(e => elementConverter(e.trim)))

  def seqHead[T:Manifest](elementConverter: TypeConverter[String, T]): TypeConverter[Seq[String], T] =
    safeOption(_.headOption.flatMap(elementConverter(_)))

  def seqToSeq[T:Manifest](elementConverter: TypeConverter[String, T]): TypeConverter[Seq[String], Seq[T]] =
    safe(_.flatMap(elementConverter(_)))

  implicit val stringSeqToBoolean: TypeConverter[Seq[String], Boolean] = seqHead(stringToBoolean)

  implicit val stringSeqToFloat: TypeConverter[Seq[String], Float] = seqHead(stringToFloat)

  implicit val stringSeqToDouble: TypeConverter[Seq[String], Double] = seqHead(stringToDouble)


  implicit val stringSeqToByte: TypeConverter[Seq[String], Byte] = seqHead(stringToByte)

  implicit val stringSeqToShort: TypeConverter[Seq[String], Short] = seqHead(stringToShort)

  implicit val stringSeqToInt: TypeConverter[Seq[String], Int] = seqHead(stringToInt)

  implicit val stringSeqToLong: TypeConverter[Seq[String], Long] = seqHead(stringToLong)

  implicit val stringSeqToString: TypeConverter[Seq[String], String] = seqHead(stringToSelf)

  implicit val stringSeqToSeqBoolean: TypeConverter[Seq[String], Seq[Boolean]] = seqToSeq(stringToBoolean)

  implicit val stringSeqToSeqFloat: TypeConverter[Seq[String], Seq[Float]] = seqToSeq(stringToFloat)

  implicit val stringSeqToSeqDouble: TypeConverter[Seq[String], Seq[Double]] = seqToSeq(stringToDouble)

  implicit val stringSeqToSeqByte: TypeConverter[Seq[String], Seq[Byte]] = seqToSeq(stringToByte)

  implicit val stringSeqToSeqShort: TypeConverter[Seq[String], Seq[Short]] = seqToSeq(stringToShort)

  implicit val stringSeqToSeqInt: TypeConverter[Seq[String], Seq[Int]] = seqToSeq(stringToInt)

  implicit val stringSeqToSeqLong: TypeConverter[Seq[String], Seq[Long]] = seqToSeq(stringToLong)

  implicit val stringSeqToSeqString: TypeConverter[Seq[String], Seq[String]] = seqToSeq(stringToSelf)

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
