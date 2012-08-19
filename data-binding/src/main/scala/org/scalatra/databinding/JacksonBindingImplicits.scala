package org.scalatra
package databinding

import org.scalatra.jackson._
import util.conversion._
import org.joda.time.DateTime
import java.util.Date
import com.fasterxml.jackson.databind.JsonNode
import util.ValueReader

trait JacksonBindingImplicits extends JacksonImplicitConversions {
  implicit def jsonToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, DateTime] =
    safeOption(s => if (s.isTextual()) df.parse(s.asText()) else None)

  implicit def jsonToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, Date] =
    safeOption(s => if (s.isTextual()) df.parse(s.asText()).map(_.toDate) else None)

  implicit def jsonToSeqDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, Seq[Date]] =
    jsonToSeq(jsonToDate)

  implicit def jsonToSeqDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, Seq[DateTime]] =
    jsonToSeq(jsonToDateTime)
}

object JacksonBindingImplicits extends JacksonBindingImplicits

trait JsonTypeConverterFactory[N, T] extends TypeConverterFactory[T]  {
  def resolveJson: TypeConverter[N, T]
}
trait JacksonTypeConverterFactory[T] extends JsonTypeConverterFactory[JsonNode, T] with JacksonBindingImplicits

trait JacksonTypeConverterFactories {

  class BooleanTypeConverterFactory extends JacksonTypeConverterFactory[Boolean] {
    def resolveJson: TypeConverter[JsonNode,  Boolean] = implicitly[TypeConverter[JsonNode,  Boolean]]
    def resolveMultiParams: TypeConverter[Seq[String], Boolean] = implicitly[TypeConverter[Seq[String], Boolean]]
    def resolveStringParams: TypeConverter[String, Boolean] = implicitly[TypeConverter[String, Boolean]]
  }
  class FloatTypeConverterFactory extends JacksonTypeConverterFactory[Float] {
    def resolveJson: TypeConverter[JsonNode,  Float] = implicitly[TypeConverter[JsonNode,  Float]]
    def resolveMultiParams: TypeConverter[Seq[String], Float] = implicitly[TypeConverter[Seq[String], Float]]
    def resolveStringParams: TypeConverter[String, Float] = implicitly[TypeConverter[String, Float]]
  }
  class DoubleTypeConverterFactory extends JacksonTypeConverterFactory[Double] {
    def resolveJson: TypeConverter[JsonNode,  Double] = implicitly[TypeConverter[JsonNode,  Double]]
    def resolveMultiParams: TypeConverter[Seq[String], Double] = implicitly[TypeConverter[Seq[String], Double]]
    def resolveStringParams: TypeConverter[String, Double] = implicitly[TypeConverter[String, Double]]
  }
  class BigDecimalTypeConverterFactory extends JacksonTypeConverterFactory[BigDecimal] with BigDecimalImplicitConversions {
    def resolveJson: TypeConverter[JsonNode,  BigDecimal] = implicitly[TypeConverter[JsonNode,  BigDecimal]]
    def resolveMultiParams: TypeConverter[Seq[String], BigDecimal] = implicitly[TypeConverter[Seq[String], BigDecimal]]
    def resolveStringParams: TypeConverter[String, BigDecimal] = implicitly[TypeConverter[String, BigDecimal]]
  }
  class ByteTypeConverterFactory extends JacksonTypeConverterFactory[Byte] {
    def resolveJson: TypeConverter[JsonNode,  Byte] = implicitly[TypeConverter[JsonNode,  Byte]]
    def resolveMultiParams: TypeConverter[Seq[String], Byte] = implicitly[TypeConverter[Seq[String], Byte]]
    def resolveStringParams: TypeConverter[String, Byte] = implicitly[TypeConverter[String, Byte]]
  }
  class ShortTypeConverterFactory extends JacksonTypeConverterFactory[Short] {
    def resolveJson: TypeConverter[JsonNode,  Short] = implicitly[TypeConverter[JsonNode,  Short]]
    def resolveMultiParams: TypeConverter[Seq[String], Short] = implicitly[TypeConverter[Seq[String], Short]]
    def resolveStringParams: TypeConverter[String, Short] = implicitly[TypeConverter[String, Short]]
  }
  class IntTypeConverterFactory extends JacksonTypeConverterFactory[Int] {
    def resolveJson: TypeConverter[JsonNode,  Int] = implicitly[TypeConverter[JsonNode,  Int]]
    def resolveMultiParams: TypeConverter[Seq[String], Int] = implicitly[TypeConverter[Seq[String], Int]]
    def resolveStringParams: TypeConverter[String, Int] = implicitly[TypeConverter[String, Int]]
  }
  class LongTypeConverterFactory extends JacksonTypeConverterFactory[Long] {
    def resolveJson: TypeConverter[JsonNode,  Long] = implicitly[TypeConverter[JsonNode,  Long]]
    def resolveMultiParams: TypeConverter[Seq[String], Long] = implicitly[TypeConverter[Seq[String], Long]]
    def resolveStringParams: TypeConverter[String, Long] = implicitly[TypeConverter[String, Long]]
  }
  class StringTypeConverterFactory extends JacksonTypeConverterFactory[String] {
    def resolveJson: TypeConverter[JsonNode,  String] = implicitly[TypeConverter[JsonNode,  String]]
    def resolveMultiParams: TypeConverter[Seq[String], String] = implicitly[TypeConverter[Seq[String], String]]
    def resolveStringParams: TypeConverter[String, String] = implicitly[TypeConverter[String, String]]
  }
  class DateTypeConverterFactory extends JacksonTypeConverterFactory[Date] {
    def resolveJson: TypeConverter[JsonNode,  Date] = implicitly[TypeConverter[JsonNode,  Date]]
    def resolveMultiParams: TypeConverter[Seq[String], Date] = implicitly[TypeConverter[Seq[String], Date]]
    def resolveStringParams: TypeConverter[String, Date] = implicitly[TypeConverter[String, Date]]
  }
  class DateTimeTypeConverterFactory extends JacksonTypeConverterFactory[DateTime] {
    def resolveJson: TypeConverter[JsonNode,  DateTime] = implicitly[TypeConverter[JsonNode,  DateTime]]
    def resolveMultiParams: TypeConverter[Seq[String], DateTime] = implicitly[TypeConverter[Seq[String], DateTime]]
    def resolveStringParams: TypeConverter[String, DateTime] = implicitly[TypeConverter[String, DateTime]]
  }
  class BooleanSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Boolean]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Boolean]] = implicitly[TypeConverter[JsonNode,  Seq[Boolean]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Boolean]] = implicitly[TypeConverter[Seq[String], Seq[Boolean]]]
    def resolveStringParams: TypeConverter[String, Seq[Boolean]] = implicitly[TypeConverter[String, Seq[Boolean]]]
  }
  class FloatSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Float]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Float]] = implicitly[TypeConverter[JsonNode,  Seq[Float]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Float]] = implicitly[TypeConverter[Seq[String], Seq[Float]]]
    def resolveStringParams: TypeConverter[String, Seq[Float]] = implicitly[TypeConverter[String, Seq[Float]]]
  }
  class DoubleSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Double]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Double]] = implicitly[TypeConverter[JsonNode,  Seq[Double]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Double]] = implicitly[TypeConverter[Seq[String], Seq[Double]]]
    def resolveStringParams: TypeConverter[String, Seq[Double]] = implicitly[TypeConverter[String, Seq[Double]]]
  }
  class BigDecimalSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[BigDecimal]] with BigDecimalImplicitConversions {
    def resolveJson: TypeConverter[JsonNode,  Seq[BigDecimal]] = implicitly[TypeConverter[JsonNode,  Seq[BigDecimal]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[BigDecimal]] = implicitly[TypeConverter[Seq[String], Seq[BigDecimal]]]
    def resolveStringParams: TypeConverter[String, Seq[BigDecimal]] = implicitly[TypeConverter[String, Seq[BigDecimal]]]
  }
  class ByteSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Byte]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Byte]] = implicitly[TypeConverter[JsonNode,  Seq[Byte]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Byte]] = implicitly[TypeConverter[Seq[String], Seq[Byte]]]
    def resolveStringParams: TypeConverter[String, Seq[Byte]] = implicitly[TypeConverter[String, Seq[Byte]]]
  }
  class ShortSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Short]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Short]] = implicitly[TypeConverter[JsonNode,  Seq[Short]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Short]] = implicitly[TypeConverter[Seq[String], Seq[Short]]]
    def resolveStringParams: TypeConverter[String, Seq[Short]] = implicitly[TypeConverter[String, Seq[Short]]]
   }
  class IntSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Int]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Int]] = implicitly[TypeConverter[JsonNode,  Seq[Int]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Int]] = implicitly[TypeConverter[Seq[String], Seq[Int]]]
    def resolveStringParams: TypeConverter[String, Seq[Int]] = implicitly[TypeConverter[String, Seq[Int]]]
  }
  class LongSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Long]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Long]] = implicitly[TypeConverter[JsonNode,  Seq[Long]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Long]] = implicitly[TypeConverter[Seq[String], Seq[Long]]]
    def resolveStringParams: TypeConverter[String, Seq[Long]] = implicitly[TypeConverter[String, Seq[Long]]]
  }
  class StringSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[String]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[String]] = implicitly[TypeConverter[JsonNode,  Seq[String]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[String]] = implicitly[TypeConverter[Seq[String], Seq[String]]]
    def resolveStringParams: TypeConverter[String, Seq[String]] = implicitly[TypeConverter[String, Seq[String]]]
  }
  class DateSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[Date]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[Date]] = implicitly[TypeConverter[JsonNode,  Seq[Date]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Date]] = implicitly[TypeConverter[Seq[String], Seq[Date]]]
    def resolveStringParams: TypeConverter[String, Seq[Date]] = implicitly[TypeConverter[String, Seq[Date]]]
  }
  class DateTimeSeqTypeConverterFactory extends JacksonTypeConverterFactory[Seq[DateTime]] {
    def resolveJson: TypeConverter[JsonNode,  Seq[DateTime]] = implicitly[TypeConverter[JsonNode,  Seq[DateTime]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[DateTime]] = implicitly[TypeConverter[Seq[String], Seq[DateTime]]]
    def resolveStringParams: TypeConverter[String, Seq[DateTime]] = implicitly[TypeConverter[String, Seq[DateTime]]]
  }

}

object JacksonTypeConverterFactories extends JacksonTypeConverterFactories

trait JacksonCommand extends JacksonTypeConverterFactoryImplicits with Command { self: Command with TypeConverterFactoryConversions =>


  type CommandTypeConverterFactory[T] = JacksonTypeConverterFactory[T]

  override def typeConverterBuilder[I](tc: CommandTypeConverterFactory[_]) = ({
    case r: JacksonValueReader => tc.resolveJson.asInstanceOf[TypeConverter[I, _]]
  }: PartialFunction[ValueReader[_, _], TypeConverter[I, _]]) orElse super.typeConverterBuilder(tc)

}

trait JacksonTypeConverterFactoryImplicits extends TypeConverterFactoryConversions with BigDecimalTypeConverterFactoryConversion {
  import JacksonTypeConverterFactories._

  implicit val booleanTypeConverterFactory: TypeConverterFactory[Boolean] = new BooleanTypeConverterFactory
  implicit val floatTypeConverterFactory: TypeConverterFactory[Float] = new FloatTypeConverterFactory
  implicit val doubleTypeConverterFactory: TypeConverterFactory[Double] = new DoubleTypeConverterFactory
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
  implicit val byteSeqTypeConverterFactory: TypeConverterFactory[Seq[Byte]] = new ByteSeqTypeConverterFactory
  implicit val shortSeqTypeConverterFactory: TypeConverterFactory[Seq[Short]] = new ShortSeqTypeConverterFactory
  implicit val intSeqTypeConverterFactory: TypeConverterFactory[Seq[Int]] = new IntSeqTypeConverterFactory
  implicit val longSeqTypeConverterFactory: TypeConverterFactory[Seq[Long]] = new LongSeqTypeConverterFactory
  implicit val stringSeqTypeConverterFactory: TypeConverterFactory[Seq[String]] = new StringSeqTypeConverterFactory
  implicit val dateSeqTypeConverterFactory: TypeConverterFactory[Seq[Date]] = new DateSeqTypeConverterFactory
  implicit val dateTimeSeqTypeConverterFactory: TypeConverterFactory[Seq[DateTime]] = new DateTimeSeqTypeConverterFactory
  implicit val bigDecimalTypeConverterFactory: TypeConverterFactory[BigDecimal] = new BigDecimalTypeConverterFactory
  implicit val bigDecimalSeqTypeConverterFactory: TypeConverterFactory[Seq[BigDecimal]] = new BigDecimalSeqTypeConverterFactory

}

trait JacksonParsing extends CommandSupport with JacksonValueReaderProperty { self: JacksonSupport with CommandSupport =>
  type CommandType = JacksonCommand

  import Imports.jacksonZero
  /**
   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  override def command[T <: CommandType](implicit mf: Manifest[T]): T = {
    commandOption[T].getOrElse {
      val newCommand = mf.erasure.newInstance.asInstanceOf[T]
      format match {
        case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
        case _ => newCommand.bindTo(multiParams, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }
}
