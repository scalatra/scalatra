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

  class BooleanTypeConverterFactory extends TypeConverterFactory[Boolean] {
    def resolveMultiParams: TypeConverter[Seq[String], Boolean] = implicitly[TypeConverter[Seq[String], Boolean]]
    def resolveStringParams: TypeConverter[String, Boolean] = implicitly[TypeConverter[String, Boolean]]
  }
  class FloatTypeConverterFactory extends TypeConverterFactory[Float] {
    def resolveMultiParams: TypeConverter[Seq[String], Float] = implicitly[TypeConverter[Seq[String], Float]]
    def resolveStringParams: TypeConverter[String, Float] = implicitly[TypeConverter[String, Float]]
  }
  class DoubleTypeConverterFactory extends TypeConverterFactory[Double] {
    def resolveMultiParams: TypeConverter[Seq[String], Double] = implicitly[TypeConverter[Seq[String], Double]]
    def resolveStringParams: TypeConverter[String, Double] = implicitly[TypeConverter[String, Double]]
  }
  class BigDecimalTypeConverterFactory extends TypeConverterFactory[BigDecimal] {
    def resolveMultiParams: TypeConverter[Seq[String], BigDecimal] = implicitly[TypeConverter[Seq[String], BigDecimal]]
    def resolveStringParams: TypeConverter[String, BigDecimal] = implicitly[TypeConverter[String, BigDecimal]]
  }
  class ByteTypeConverterFactory extends TypeConverterFactory[Byte] {
    def resolveMultiParams: TypeConverter[Seq[String], Byte] = implicitly[TypeConverter[Seq[String], Byte]]
    def resolveStringParams: TypeConverter[String, Byte] = implicitly[TypeConverter[String, Byte]]
  }
  class ShortTypeConverterFactory extends TypeConverterFactory[Short] {
    def resolveMultiParams: TypeConverter[Seq[String], Short] = implicitly[TypeConverter[Seq[String], Short]]
    def resolveStringParams: TypeConverter[String, Short] = implicitly[TypeConverter[String, Short]]
  }
  class IntTypeConverterFactory extends TypeConverterFactory[Int] {
    def resolveMultiParams: TypeConverter[Seq[String], Int] = implicitly[TypeConverter[Seq[String], Int]]
    def resolveStringParams: TypeConverter[String, Int] = implicitly[TypeConverter[String, Int]]
  }
  class LongTypeConverterFactory extends TypeConverterFactory[Long] {
    def resolveMultiParams: TypeConverter[Seq[String], Long] = implicitly[TypeConverter[Seq[String], Long]]
    def resolveStringParams: TypeConverter[String, Long] = implicitly[TypeConverter[String, Long]]
  }
  class StringTypeConverterFactory extends TypeConverterFactory[String] {
    def resolveMultiParams: TypeConverter[Seq[String], String] = implicitly[TypeConverter[Seq[String], String]]
    def resolveStringParams: TypeConverter[String, String] = implicitly[TypeConverter[String, String]]
  }
  class DateTypeConverterFactory extends TypeConverterFactory[Date] {
    def resolveMultiParams: TypeConverter[Seq[String], Date] = implicitly[TypeConverter[Seq[String], Date]]
    def resolveStringParams: TypeConverter[String, Date] = implicitly[TypeConverter[String, Date]]
  }
  class DateTimeTypeConverterFactory extends TypeConverterFactory[DateTime] {
    def resolveMultiParams: TypeConverter[Seq[String], DateTime] = implicitly[TypeConverter[Seq[String], DateTime]]
    def resolveStringParams: TypeConverter[String, DateTime] = implicitly[TypeConverter[String, DateTime]]
  }
  class BooleanSeqTypeConverterFactory extends TypeConverterFactory[Seq[Boolean]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Boolean]] = implicitly[TypeConverter[Seq[String], Seq[Boolean]]]
    def resolveStringParams: TypeConverter[String, Seq[Boolean]] = implicitly[TypeConverter[String, Seq[Boolean]]]
  }
  class FloatSeqTypeConverterFactory extends TypeConverterFactory[Seq[Float]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Float]] = implicitly[TypeConverter[Seq[String], Seq[Float]]]
    def resolveStringParams: TypeConverter[String, Seq[Float]] = implicitly[TypeConverter[String, Seq[Float]]]
  }
  class DoubleSeqTypeConverterFactory extends TypeConverterFactory[Seq[Double]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Double]] = implicitly[TypeConverter[Seq[String], Seq[Double]]]
    def resolveStringParams: TypeConverter[String, Seq[Double]] = implicitly[TypeConverter[String, Seq[Double]]]
  }
  class BigDecimalSeqTypeConverterFactory extends TypeConverterFactory[Seq[BigDecimal]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[BigDecimal]] = implicitly[TypeConverter[Seq[String], Seq[BigDecimal]]]
    def resolveStringParams: TypeConverter[String, Seq[BigDecimal]] = implicitly[TypeConverter[String, Seq[BigDecimal]]]
  }
  class ByteSeqTypeConverterFactory extends TypeConverterFactory[Seq[Byte]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Byte]] = implicitly[TypeConverter[Seq[String], Seq[Byte]]]
    def resolveStringParams: TypeConverter[String, Seq[Byte]] = implicitly[TypeConverter[String, Seq[Byte]]]
  }
  class ShortSeqTypeConverterFactory extends TypeConverterFactory[Seq[Short]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Short]] = implicitly[TypeConverter[Seq[String], Seq[Short]]]
    def resolveStringParams: TypeConverter[String, Seq[Short]] = implicitly[TypeConverter[String, Seq[Short]]]
   }
  class IntSeqTypeConverterFactory extends TypeConverterFactory[Seq[Int]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Int]] = implicitly[TypeConverter[Seq[String], Seq[Int]]]
    def resolveStringParams: TypeConverter[String, Seq[Int]] = implicitly[TypeConverter[String, Seq[Int]]]
  }
  class LongSeqTypeConverterFactory extends TypeConverterFactory[Seq[Long]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Long]] = implicitly[TypeConverter[Seq[String], Seq[Long]]]
    def resolveStringParams: TypeConverter[String, Seq[Long]] = implicitly[TypeConverter[String, Seq[Long]]]
  }
  class StringSeqTypeConverterFactory extends TypeConverterFactory[Seq[String]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[String]] = implicitly[TypeConverter[Seq[String], Seq[String]]]
    def resolveStringParams: TypeConverter[String, Seq[String]] = implicitly[TypeConverter[String, Seq[String]]]
  }
  class DateSeqTypeConverterFactory extends TypeConverterFactory[Seq[Date]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Date]] = implicitly[TypeConverter[Seq[String], Seq[Date]]]
    def resolveStringParams: TypeConverter[String, Seq[Date]] = implicitly[TypeConverter[String, Seq[Date]]]
  }
  class DateTimeSeqTypeConverterFactory extends TypeConverterFactory[Seq[DateTime]] {
    def resolveMultiParams: TypeConverter[Seq[String], Seq[DateTime]] = implicitly[TypeConverter[Seq[String], Seq[DateTime]]]
    def resolveStringParams: TypeConverter[String, Seq[DateTime]] = implicitly[TypeConverter[String, Seq[DateTime]]]
  }

}

trait TypeConverterFactoryConversions {
  implicit def booleanTypeConverterFactory: TypeConverterFactory[Boolean] 
  implicit def floatTypeConverterFactory: TypeConverterFactory[Float] 
  implicit def doubleTypeConverterFactory: TypeConverterFactory[Double]
  implicit def bigDecimalTypeConverterFactory: TypeConverterFactory[BigDecimal] 
  implicit def byteTypeConverterFactory: TypeConverterFactory[Byte] 
  implicit def shortTypeConverterFactory: TypeConverterFactory[Short] 
  implicit def intTypeConverterFactory: TypeConverterFactory[Int] 
  implicit def longTypeConverterFactory: TypeConverterFactory[Long] 
  implicit def stringTypeConverterFactory: TypeConverterFactory[String] 
  implicit def dateTypeConverterFactory: TypeConverterFactory[Date] 
  implicit def dateTimeTypeConverterFactory: TypeConverterFactory[DateTime] 
  implicit def booleanSeqTypeConverterFactory: TypeConverterFactory[Seq[Boolean]] 
  implicit def floatSeqTypeConverterFactory: TypeConverterFactory[Seq[Float]] 
  implicit def doubleSeqTypeConverterFactory: TypeConverterFactory[Seq[Double]] 
  implicit def bigDecimalSeqTypeConverterFactory: TypeConverterFactory[Seq[BigDecimal]] 
  implicit def byteSeqTypeConverterFactory: TypeConverterFactory[Seq[Byte]] 
  implicit def shortSeqTypeConverterFactory: TypeConverterFactory[Seq[Short]] 
  implicit def intSeqTypeConverterFactory: TypeConverterFactory[Seq[Int]] 
  implicit def longSeqTypeConverterFactory: TypeConverterFactory[Seq[Long]] 
  implicit def stringSeqTypeConverterFactory: TypeConverterFactory[Seq[String]] 
  implicit def dateSeqTypeConverterFactory: TypeConverterFactory[Seq[Date]] 
  implicit def dateTimeSeqTypeConverterFactory: TypeConverterFactory[Seq[DateTime]]
}

trait TypeConverterFactoryImplicits extends TypeConverterFactoryConversions {
  import TypeConverterFactories._
  
  implicit val booleanTypeConverterFactory: TypeConverterFactory[Boolean] = new BooleanTypeConverterFactory
  implicit val floatTypeConverterFactory: TypeConverterFactory[Float] = new FloatTypeConverterFactory
  implicit val doubleTypeConverterFactory: TypeConverterFactory[Double] = new DoubleTypeConverterFactory
  implicit val bigDecimalTypeConverterFactory: TypeConverterFactory[BigDecimal] = new BigDecimalTypeConverterFactory
  implicit val byteTypeConverterFactory: TypeConverterFactory[Byte] = new ByteTypeConverterFactory
  implicit val shortTypeConverterFactory: TypeConverterFactory[Short] = new ShortTypeConverterFactory
  implicit val intTypeConverterFactory: TypeConverterFactory[Int] = new IntTypeConverterFactory
  implicit val longTypeConverterFactory: TypeConverterFactory[Long] = new LongTypeConverterFactory
  implicit val stringTypeConverterFactory: TypeConverterFactory[String] = new StringTypeConverterFactory
  implicit val dateTypeConverterFactory: TypeConverterFactory[Date] = new DateTypeConverterFactory
  implicit val dateTimeTypeConverterFactory: TypeConverterFactory[DateTime] = new DateTimeTypeConverterFactory
  implicit val booleanSeqTypeConverterFactory: TypeConverterFactory[Seq[Boolean]] = new BooleanSeqTypeConverterFactory
  implicit val floatSeqTypeConverterFactory: TypeConverterFactory[Seq[Float]] = new FloatSeqTypeConverterFactory
  implicit val doubleSeqTypeConverterFactory: TypeConverterFactory[Seq[Double]] = new DoubleSeqTypeConverterFactory
  implicit val bigDecimalSeqTypeConverterFactory: TypeConverterFactory[Seq[BigDecimal]] = new BigDecimalSeqTypeConverterFactory
  implicit val byteSeqTypeConverterFactory: TypeConverterFactory[Seq[Byte]] = new ByteSeqTypeConverterFactory
  implicit val shortSeqTypeConverterFactory: TypeConverterFactory[Seq[Short]] = new ShortSeqTypeConverterFactory
  implicit val intSeqTypeConverterFactory: TypeConverterFactory[Seq[Int]] = new IntSeqTypeConverterFactory
  implicit val longSeqTypeConverterFactory: TypeConverterFactory[Seq[Long]] = new LongSeqTypeConverterFactory
  implicit val stringSeqTypeConverterFactory: TypeConverterFactory[Seq[String]] = new StringSeqTypeConverterFactory
  implicit val dateSeqTypeConverterFactory: TypeConverterFactory[Seq[Date]] = new DateSeqTypeConverterFactory
  implicit val dateTimeSeqTypeConverterFactory: TypeConverterFactory[Seq[DateTime]] = new DateTimeSeqTypeConverterFactory
}

object TypeConverterFactoryImplicits extends TypeConverterFactoryImplicits
object TypeConverterFactories extends TypeConverterFactories