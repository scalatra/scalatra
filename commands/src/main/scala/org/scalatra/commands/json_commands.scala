package org.scalatra
package commands

import org.json4s._
import org.scalatra.json.{ NativeJsonSupport, NativeJsonValueReaderProperty, JsonValueReader, JsonImplicitConversions }
import org.scalatra.util.conversion._
import org.joda.time.DateTime
import java.util.Date
import org.scalatra.util.ValueReader

trait JsonBindingImplicits extends BindingImplicits with JsonImplicitConversions {

  implicit def jsonToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, DateTime] =
    safeOption(_.extractOpt[String].flatMap(df.parse))

  implicit def jsonToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, Date] =
    safeOption(_.extractOpt[String].flatMap(df.parse).map(_.toDate))

}

trait JsonTypeConverterFactory[T] extends TypeConverterFactory[T] with JsonBindingImplicits {
  def resolveJson: TypeConverter[JValue, T]
}

trait JsonTypeConverterFactories extends JsonBindingImplicits {
  implicit def jsonTypeConverterFactory[T](implicit seqConverter: TypeConverter[Seq[String], T],
    stringConverter: TypeConverter[String, T],
    jsonConverter: TypeConverter[JValue, T],
    formats: Formats): TypeConverterFactory[T] =
    new JsonTypeConverterFactory[T] {
      implicit protected val jsonFormats: Formats = formats
      def resolveJson: TypeConverter[JValue, T] = jsonConverter
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
