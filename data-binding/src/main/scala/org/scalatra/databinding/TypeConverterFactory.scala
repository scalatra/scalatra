package org.scalatra
package databinding

import util.conversion._
import java.util.Date
import org.joda.time.DateTime

trait TypeConverterFactory[T] extends BindingImplicits {

  def resolveMultiParams: TypeConverter[Seq[String], T]
  def resolveStringParams: TypeConverter[String, T]
}

trait TypeConverterFactories {

  implicit object BooleanTypeConverterFactory extends TypeConverterFactory[Boolean] {
    def resolveMultiParams: TypeConverter[Seq[String], Boolean] = implicitly[TypeConverter[Seq[String], Boolean]]
    def resolveStringParams: TypeConverter[String, Boolean] = implicitly[TypeConverter[String, Boolean]]
  }
  implicit object FloatTypeConverterFactory extends TypeConverterFactory[Float] {
    def resolveMultiParams: TypeConverter[Seq[String], Float] = implicitly[TypeConverter[Seq[String], Float]]
    def resolveStringParams: TypeConverter[String, Float] = implicitly[TypeConverter[String, Float]]
  }
  implicit object DoubleTypeConverterFactory extends TypeConverterFactory[Double] {
    def resolveMultiParams: TypeConverter[Seq[String], Double] = implicitly[TypeConverter[Seq[String], Double]]
    def resolveStringParams: TypeConverter[String, Double] = implicitly[TypeConverter[String, Double]]
  }
  implicit object BigDecimalTypeConverterFactory extends TypeConverterFactory[BigDecimal] {
    def resolveMultiParams: TypeConverter[Seq[String], BigDecimal] = implicitly[TypeConverter[Seq[String], BigDecimal]]
    def resolveStringParams: TypeConverter[String, BigDecimal] = implicitly[TypeConverter[String, BigDecimal]]
  }
  implicit object ByteTypeConverterFactory extends TypeConverterFactory[Byte] {
    def resolveMultiParams: TypeConverter[Seq[String], Byte] = implicitly[TypeConverter[Seq[String], Byte]]
    def resolveStringParams: TypeConverter[String, Byte] = implicitly[TypeConverter[String, Byte]]
  }
  implicit object ShortTypeConverterFactory extends TypeConverterFactory[Short] {
    def resolveMultiParams: TypeConverter[Seq[String], Short] = implicitly[TypeConverter[Seq[String], Short]]
    def resolveStringParams: TypeConverter[String, Short] = implicitly[TypeConverter[String, Short]]
  }
  implicit object IntTypeConverterFactory extends TypeConverterFactory[Int] {
    def resolveMultiParams: TypeConverter[Seq[String], Int] = implicitly[TypeConverter[Seq[String], Int]]
    def resolveStringParams: TypeConverter[String, Int] = implicitly[TypeConverter[String, Int]]
  }
  implicit object LongTypeConverterFactory extends TypeConverterFactory[Long] {
    def resolveMultiParams: TypeConverter[Seq[String], Long] = implicitly[TypeConverter[Seq[String], Long]]
    def resolveStringParams: TypeConverter[String, Long] = implicitly[TypeConverter[String, Long]]
  }
  implicit object StringTypeConverterFactory extends TypeConverterFactory[String] {
    def resolveMultiParams: TypeConverter[Seq[String], String] = implicitly[TypeConverter[Seq[String], String]]
    def resolveStringParams: TypeConverter[String, String] = implicitly[TypeConverter[String, String]]
  }
  implicit object DateTypeConverterFactory extends TypeConverterFactory[Date] {
    def resolveMultiParams: TypeConverter[Seq[String], Date] = implicitly[TypeConverter[Seq[String], Date]]
    def resolveStringParams: TypeConverter[String, Date] = implicitly[TypeConverter[String, Date]]
  }
  implicit object DateTimeTypeConverterFactory extends TypeConverterFactory[DateTime] {
    def resolveMultiParams: TypeConverter[Seq[String], DateTime] = implicitly[TypeConverter[Seq[String], DateTime]]
    def resolveStringParams: TypeConverter[String, DateTime] = implicitly[TypeConverter[String, DateTime]]
  }
  implicit object BooleanSeqTypeConverterFactory extends TypeConverterFactory[Seq[Boolean]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Boolean]] = implicitly[TypeConverter[Seq[String], Seq[Boolean]]]
    def resolveStringParams: TypeConverter[String, Seq[Boolean]] = implicitly[TypeConverter[String, Seq[Boolean]]]
  }
  implicit object FloatSeqTypeConverterFactory extends TypeConverterFactory[Seq[Float]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Float]] = implicitly[TypeConverter[Seq[String], Seq[Float]]]
    def resolveStringParams: TypeConverter[String, Seq[Float]] = implicitly[TypeConverter[String, Seq[Float]]]
  }
  implicit object DoubleSeqTypeConverterFactory extends TypeConverterFactory[Seq[Double]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Double]] = implicitly[TypeConverter[Seq[String], Seq[Double]]]
    def resolveStringParams: TypeConverter[String, Seq[Double]] = implicitly[TypeConverter[String, Seq[Double]]]
  }
  implicit object BigDecimalSeqTypeConverterFactory extends TypeConverterFactory[Seq[BigDecimal]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[BigDecimal]] = implicitly[TypeConverter[Seq[String], Seq[BigDecimal]]]
    def resolveStringParams: TypeConverter[String, Seq[BigDecimal]] = implicitly[TypeConverter[String, Seq[BigDecimal]]]
  }
  implicit object ByteSeqTypeConverterFactory extends TypeConverterFactory[Seq[Byte]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Byte]] = implicitly[TypeConverter[Seq[String], Seq[Byte]]]
    def resolveStringParams: TypeConverter[String, Seq[Byte]] = implicitly[TypeConverter[String, Seq[Byte]]]
  }
  implicit object ShortSeqTypeConverterFactory extends TypeConverterFactory[Seq[Short]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Short]] = implicitly[TypeConverter[Seq[String], Seq[Short]]]
    def resolveStringParams: TypeConverter[String, Seq[Short]] = implicitly[TypeConverter[String, Seq[Short]]]
   }
  implicit object IntSeqTypeConverterFactory extends TypeConverterFactory[Seq[Int]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Int]] = implicitly[TypeConverter[Seq[String], Seq[Int]]]
    def resolveStringParams: TypeConverter[String, Seq[Int]] = implicitly[TypeConverter[String, Seq[Int]]]
  }
  implicit object LongSeqTypeConverterFactory extends TypeConverterFactory[Seq[Long]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Long]] = implicitly[TypeConverter[Seq[String], Seq[Long]]]
    def resolveStringParams: TypeConverter[String, Seq[Long]] = implicitly[TypeConverter[String, Seq[Long]]]
  }
  implicit object StringSeqTypeConverterFactory extends TypeConverterFactory[Seq[String]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[String]] = implicitly[TypeConverter[Seq[String], Seq[String]]]
    def resolveStringParams: TypeConverter[String, Seq[String]] = implicitly[TypeConverter[String, Seq[String]]]
  }
  implicit object DateTypeSeqConverterFactory extends TypeConverterFactory[Seq[Date]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Date]] = implicitly[TypeConverter[Seq[String], Seq[Date]]]
    def resolveStringParams: TypeConverter[String, Seq[Date]] = implicitly[TypeConverter[String, Seq[Date]]]
  }
  implicit object DateTimeSeqTypeConverterFactory extends TypeConverterFactory[Seq[DateTime]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[DateTime]] = implicitly[TypeConverter[Seq[String], Seq[DateTime]]]
    def resolveStringParams: TypeConverter[String, Seq[DateTime]] = implicitly[TypeConverter[String, Seq[DateTime]]]
  }

}
object TypeConverterFactory extends TypeConverterFactories