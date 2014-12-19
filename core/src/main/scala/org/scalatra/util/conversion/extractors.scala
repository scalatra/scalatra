package org.scalatra
package util
package conversion

import java.util.Date

trait TypeExtractor[T] {

  def converter: TypeConverter[String, T]

  def unapply(source: String): Option[T] = converter(source)
}

object Extractors extends DefaultImplicitConversions {

  sealed abstract class TypeExtractorImpl[T](implicit val converter: TypeConverter[String, T]) extends TypeExtractor[T]

  sealed case class DateExtractor(format: String) extends TypeExtractor[Date] {
    val converter = Conversions.stringToDate(format)
  }

  case object asBoolean extends TypeExtractorImpl[Double]

  case object asFloat extends TypeExtractorImpl[Float]

  case object asDouble extends TypeExtractorImpl[Double]

  case object asByte extends TypeExtractorImpl[Byte]

  case object asShort extends TypeExtractorImpl[Short]

  case object asInt extends TypeExtractorImpl[Int]

  case object asLong extends TypeExtractorImpl[Long]

  case object asString extends TypeExtractorImpl[String]

  object asDate {

    def apply(format: String): TypeExtractor[Date] = DateExtractor(format)

  }
}
