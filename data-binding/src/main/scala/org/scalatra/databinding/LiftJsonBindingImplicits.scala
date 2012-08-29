package org.scalatra.databinding

import org.json4s._
import org.scalatra.json.{NativeJsonSupport, NativeJsonValueReaderProperty, JsonValueReader, JsonImplicitConversions}
import org.scalatra.util.conversion._
import org.joda.time.DateTime
import java.util.Date
import org.scalatra.util.ValueReader
import scalaz._
import Scalaz._

class JsonBindingImports(implicit protected val jsonFormats: Formats) extends JsonBindingImplicits
trait JsonBindingImplicits extends JsonImplicitConversions {

  implicit def jsonToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, DateTime] =
      safeOption(_.extractOpt[String].flatMap(df.parse))

  implicit def jsonToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, Date] =
    safeOption(_.extractOpt[String].flatMap(df.parse).map(_.toDate))

  implicit def jsonToSeqDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, Seq[Date]] =
    jsonToSeq(jsonToDate)

  implicit def jsonToSeqDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, Seq[DateTime]] =
    jsonToSeq(jsonToDateTime)

}

trait JsonTypeConverterFactory[T] extends TypeConverterFactory[T] with JsonBindingImplicits {
  def resolveJson: TypeConverter[JValue, T]
}
trait JsonTypeConverterFactories {
  protected implicit def jsonFormats: Formats
  class BooleanTypeConverterFactory extends JsonTypeConverterFactory[Boolean] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Boolean] = implicitly[TypeConverter[JValue,  Boolean]]
    def resolveMultiParams: TypeConverter[Seq[String], Boolean] = implicitly[TypeConverter[Seq[String], Boolean]]
    def resolveStringParams: TypeConverter[String, Boolean] = implicitly[TypeConverter[String, Boolean]]
  }
  class FloatTypeConverterFactory extends JsonTypeConverterFactory[Float] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Float] = implicitly[TypeConverter[JValue,  Float]]
    def resolveMultiParams: TypeConverter[Seq[String], Float] = implicitly[TypeConverter[Seq[String], Float]]
    def resolveStringParams: TypeConverter[String, Float] = implicitly[TypeConverter[String, Float]]
  }
  class DoubleTypeConverterFactory extends JsonTypeConverterFactory[Double] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Double] = implicitly[TypeConverter[JValue,  Double]]
    def resolveMultiParams: TypeConverter[Seq[String], Double] = implicitly[TypeConverter[Seq[String], Double]]
    def resolveStringParams: TypeConverter[String, Double] = implicitly[TypeConverter[String, Double]]
  }
  class ByteTypeConverterFactory extends JsonTypeConverterFactory[Byte] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Byte] = implicitly[TypeConverter[JValue,  Byte]]
    def resolveMultiParams: TypeConverter[Seq[String], Byte] = implicitly[TypeConverter[Seq[String], Byte]]
    def resolveStringParams: TypeConverter[String, Byte] = implicitly[TypeConverter[String, Byte]]
  }
  class ShortTypeConverterFactory extends JsonTypeConverterFactory[Short] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Short] = implicitly[TypeConverter[JValue,  Short]]
    def resolveMultiParams: TypeConverter[Seq[String], Short] = implicitly[TypeConverter[Seq[String], Short]]
    def resolveStringParams: TypeConverter[String, Short] = implicitly[TypeConverter[String, Short]]
  }
  class IntTypeConverterFactory extends JsonTypeConverterFactory[Int] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Int] = implicitly[TypeConverter[JValue,  Int]]
    def resolveMultiParams: TypeConverter[Seq[String], Int] = implicitly[TypeConverter[Seq[String], Int]]
    def resolveStringParams: TypeConverter[String, Int] = implicitly[TypeConverter[String, Int]]
  }
  class LongTypeConverterFactory extends JsonTypeConverterFactory[Long] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Long] = implicitly[TypeConverter[JValue,  Long]]
    def resolveMultiParams: TypeConverter[Seq[String], Long] = implicitly[TypeConverter[Seq[String], Long]]
    def resolveStringParams: TypeConverter[String, Long] = implicitly[TypeConverter[String, Long]]
  }
  class StringTypeConverterFactory extends JsonTypeConverterFactory[String] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  String] = implicitly[TypeConverter[JValue,  String]]
    def resolveMultiParams: TypeConverter[Seq[String], String] = implicitly[TypeConverter[Seq[String], String]]
    def resolveStringParams: TypeConverter[String, String] = implicitly[TypeConverter[String, String]]
  }
  class DateTypeConverterFactory extends JsonTypeConverterFactory[Date] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Date] = implicitly[TypeConverter[JValue,  Date]]
    def resolveMultiParams: TypeConverter[Seq[String], Date] = implicitly[TypeConverter[Seq[String], Date]]
    def resolveStringParams: TypeConverter[String, Date] = implicitly[TypeConverter[String, Date]]
  }
  class DateTimeTypeConverterFactory extends JsonTypeConverterFactory[DateTime] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  DateTime] = implicitly[TypeConverter[JValue,  DateTime]]
    def resolveMultiParams: TypeConverter[Seq[String], DateTime] = implicitly[TypeConverter[Seq[String], DateTime]]
    def resolveStringParams: TypeConverter[String, DateTime] = implicitly[TypeConverter[String, DateTime]]
  }
  class BooleanSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Boolean]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Boolean]] = implicitly[TypeConverter[JValue,  Seq[Boolean]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Boolean]] = implicitly[TypeConverter[Seq[String], Seq[Boolean]]]
    def resolveStringParams: TypeConverter[String, Seq[Boolean]] = implicitly[TypeConverter[String, Seq[Boolean]]]
  }
  class FloatSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Float]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Float]] = implicitly[TypeConverter[JValue,  Seq[Float]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Float]] = implicitly[TypeConverter[Seq[String], Seq[Float]]]
    def resolveStringParams: TypeConverter[String, Seq[Float]] = implicitly[TypeConverter[String, Seq[Float]]]
  }
  class DoubleSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Double]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Double]] = implicitly[TypeConverter[JValue,  Seq[Double]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Double]] = implicitly[TypeConverter[Seq[String], Seq[Double]]]
    def resolveStringParams: TypeConverter[String, Seq[Double]] = implicitly[TypeConverter[String, Seq[Double]]]
  }
  class ByteSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Byte]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Byte]] = implicitly[TypeConverter[JValue,  Seq[Byte]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Byte]] = implicitly[TypeConverter[Seq[String], Seq[Byte]]]
    def resolveStringParams: TypeConverter[String, Seq[Byte]] = implicitly[TypeConverter[String, Seq[Byte]]]
  }
  class ShortSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Short]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Short]] = implicitly[TypeConverter[JValue,  Seq[Short]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Short]] = implicitly[TypeConverter[Seq[String], Seq[Short]]]
    def resolveStringParams: TypeConverter[String, Seq[Short]] = implicitly[TypeConverter[String, Seq[Short]]]
   }
  class IntSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Int]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Int]] = implicitly[TypeConverter[JValue,  Seq[Int]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Int]] = implicitly[TypeConverter[Seq[String], Seq[Int]]]
    def resolveStringParams: TypeConverter[String, Seq[Int]] = implicitly[TypeConverter[String, Seq[Int]]]
  }
  class LongSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Long]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Long]] = implicitly[TypeConverter[JValue,  Seq[Long]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Long]] = implicitly[TypeConverter[Seq[String], Seq[Long]]]
    def resolveStringParams: TypeConverter[String, Seq[Long]] = implicitly[TypeConverter[String, Seq[Long]]]
  }
  class StringSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[String]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[String]] = implicitly[TypeConverter[JValue,  Seq[String]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[String]] = implicitly[TypeConverter[Seq[String], Seq[String]]]
    def resolveStringParams: TypeConverter[String, Seq[String]] = implicitly[TypeConverter[String, Seq[String]]]
  }
  class DateSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[Date]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[Date]] = implicitly[TypeConverter[JValue,  Seq[Date]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[Date]] = implicitly[TypeConverter[Seq[String], Seq[Date]]]
    def resolveStringParams: TypeConverter[String, Seq[Date]] = implicitly[TypeConverter[String, Seq[Date]]]
  }
  class DateTimeSeqTypeConverterFactory extends JsonTypeConverterFactory[Seq[DateTime]] {
    implicit protected val jsonFormats: Formats = JsonTypeConverterFactories.this.jsonFormats
    def resolveJson: TypeConverter[JValue,  Seq[DateTime]] = implicitly[TypeConverter[JValue,  Seq[DateTime]]]
    def resolveMultiParams: TypeConverter[Seq[String], Seq[DateTime]] = implicitly[TypeConverter[Seq[String], Seq[DateTime]]]
    def resolveStringParams: TypeConverter[String, Seq[DateTime]] = implicitly[TypeConverter[String, Seq[DateTime]]]
  }

}
class JsonTypeConverterFactoriesImpl(implicit protected val jsonFormats: Formats) extends JsonTypeConverterFactories

trait JsonCommand extends JsonTypeConverterFactories with JsonTypeConverterFactoryImplicits with Command { self: Command with TypeConverterFactoryConversions =>

  protected implicit def jsonFormats: Formats

  type CommandTypeConverterFactory[T] = JsonTypeConverterFactory[T]

  override def typeConverterBuilder[I](tc: CommandTypeConverterFactory[_]) = ({
    case r: JsonValueReader => tc.resolveJson.asInstanceOf[TypeConverter[I, _]]
  }: PartialFunction[ValueReader[_, _], TypeConverter[I, _]]) orElse super.typeConverterBuilder(tc)

}

trait JsonTypeConverterFactoryImplicits extends TypeConverterFactoryConversions {
  protected implicit def jsonFormats: Formats
  private[this] val ljtcf = new JsonTypeConverterFactoriesImpl()(jsonFormats)


  implicit val booleanTypeConverterFactory: TypeConverterFactory[Boolean] = new ljtcf.BooleanTypeConverterFactory
  implicit val floatTypeConverterFactory: TypeConverterFactory[Float] = new ljtcf.FloatTypeConverterFactory
  implicit val doubleTypeConverterFactory: TypeConverterFactory[Double] = new ljtcf.DoubleTypeConverterFactory
  implicit val byteTypeConverterFactory: TypeConverterFactory[Byte] = new ljtcf.ByteTypeConverterFactory
  implicit val shortTypeConverterFactory: TypeConverterFactory[Short] = new ljtcf.ShortTypeConverterFactory
  implicit val intTypeConverterFactory: TypeConverterFactory[Int] = new ljtcf.IntTypeConverterFactory
  implicit val longTypeConverterFactory: TypeConverterFactory[Long] = new ljtcf.LongTypeConverterFactory
  implicit val stringTypeConverterFactory: TypeConverterFactory[String] = new ljtcf.StringTypeConverterFactory
  implicit val dateTypeConverterFactory: TypeConverterFactory[Date] = new ljtcf.DateTypeConverterFactory
  implicit val dateTimeTypeConverterFactory: TypeConverterFactory[DateTime] = new ljtcf.DateTimeTypeConverterFactory
  implicit val booleanSeqTypeConverterFactory: TypeConverterFactory[Seq[Boolean]] = new ljtcf.BooleanSeqTypeConverterFactory
  implicit val floatSeqTypeConverterFactory: TypeConverterFactory[Seq[Float]] = new ljtcf.FloatSeqTypeConverterFactory
  implicit val doubleSeqTypeConverterFactory: TypeConverterFactory[Seq[Double]] = new ljtcf.DoubleSeqTypeConverterFactory
  implicit val byteSeqTypeConverterFactory: TypeConverterFactory[Seq[Byte]] = new ljtcf.ByteSeqTypeConverterFactory
  implicit val shortSeqTypeConverterFactory: TypeConverterFactory[Seq[Short]] = new ljtcf.ShortSeqTypeConverterFactory
  implicit val intSeqTypeConverterFactory: TypeConverterFactory[Seq[Int]] = new ljtcf.IntSeqTypeConverterFactory
  implicit val longSeqTypeConverterFactory: TypeConverterFactory[Seq[Long]] = new ljtcf.LongSeqTypeConverterFactory
  implicit val stringSeqTypeConverterFactory: TypeConverterFactory[Seq[String]] = new ljtcf.StringSeqTypeConverterFactory
  implicit val dateSeqTypeConverterFactory: TypeConverterFactory[Seq[Date]] = new ljtcf.DateSeqTypeConverterFactory
  implicit val dateTimeSeqTypeConverterFactory: TypeConverterFactory[Seq[DateTime]] = new ljtcf.DateTimeSeqTypeConverterFactory

}


trait JsonZeroes {
  implicit val liftJsonZero: Zero[JValue] = zero(JNothing)
}
object JsonZeroes extends JsonZeroes
trait NativeJsonParsing extends CommandSupport with NativeJsonValueReaderProperty { self: NativeJsonSupport with CommandSupport =>
  type CommandType = JsonCommand
  import JsonZeroes._

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
        case _ => newCommand.bindTo(params, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }

}