package org.scalatra.databinding

import org.json4s._
import org.scalatra.json.{NativeJsonSupport, NativeJsonValueReaderProperty, JsonValueReader, JsonImplicitConversions}
import org.scalatra.util.conversion._
import org.joda.time.DateTime
import java.util.Date
import org.scalatra.util.ValueReader
import scalaz._
import Scalaz._

trait JsonBindingImplicits extends BindingImplicits with JsonImplicitConversions {

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

trait JsonTypeConverterFactories extends JsonBindingImplicits {
  implicit def jsonTypeConverterFactory[T](implicit
                                           seqConverter: TypeConverter[Seq[String], T],
                                           stringConverter: TypeConverter[String, T],
                                           jsonConverter: TypeConverter[JValue, T],
                                           formats: Formats): TypeConverterFactory[T] =
  new JsonTypeConverterFactory[T] {
    implicit protected val jsonFormats: Formats = formats
    def resolveJson: TypeConverter[JValue,  T] = jsonConverter
    def resolveMultiParams: TypeConverter[Seq[String], T] = seqConverter
    def resolveStringParams: TypeConverter[String, T] = stringConverter
  }
}

class JsonTypeConverterFactoriesImports(implicit protected val jsonFormats: Formats) extends JsonTypeConverterFactories

trait JsonCommand extends Command with JsonTypeConverterFactories {

  type CommandTypeConverterFactory[T] = JsonTypeConverterFactory[T]

  override def typeConverterBuilder[I](tc: CommandTypeConverterFactory[_]) = ({
    case r: JsonValueReader => tc.resolveJson.asInstanceOf[TypeConverter[I, _]]
  }: PartialFunction[ValueReader[_, _], TypeConverter[I, _]]) orElse super.typeConverterBuilder(tc)

}

trait JsonZeroes {
  implicit val liftJsonZero: Zero[JValue] = zero(JNothing)
}
object JsonZeroes extends JsonZeroes
trait NativeJsonParsing extends CommandSupport with NativeJsonValueReaderProperty { self: NativeJsonSupport with CommandSupport =>
  type CommandType = JsonCommand
  import JsonZeroes._

  /**
   * Create and bind a [[org.scalatra.databinding.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  override def command[T <: CommandType](implicit mf: Manifest[T]): T = {
    commandOption[T] getOrElse {
      val newCommand = mf.erasure.newInstance.asInstanceOf[T]
      format match {
        case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
        case _ => newCommand.bindTo(params, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }

  /**
   * Create and bind a [[org.scalatra.databinding.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  override def commandOrElse[T <: CommandType](factory: ⇒ T)(implicit mf: Manifest[T]): T = {
    commandOption[T] getOrElse {
      val newCommand = factory
      format match {
        case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
        case _ => newCommand.bindTo(params, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }


}