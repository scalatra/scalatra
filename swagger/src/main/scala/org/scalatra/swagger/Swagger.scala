package org.scalatra
package swagger

import java.util.{Date => JDate, Locale}
import org.json4s._
import ext.{ JodaTimeSerializers, EnumNameSerializer }
import org.joda.time._
import format.ISODateTimeFormat
import grizzled.slf4j.Logger

/**
 * An instance of this class is used to hold the API documentation.
 */
class Swagger(val swaggerVersion: String, val apiVersion: String) {
  private[this] val logger = Logger[this.type]
  private var _docs = Map.empty[String, Api]

  def docs = _docs.values

  /**
   * Returns the documentation for the given path.
   */
  def doc(path: String): Option[Api] = _docs.get(path)

  /**
   * Registers the documentation for an API with the given path.
   */
  def register(name: String, path: String, description: String, s: SwaggerSupport, listingPath: Option[String] = None) = {
    logger.debug("registering swagger api with: { name: %s, path: %s, description: %s, servlet: %s, listingPath: %s }" format (name, path, description, s.getClass, listingPath))
    _docs = _docs + (name -> Api(path, listingPath, description, s.endpoints(path), s.models))
  }
}

case class Api(resourcePath: String,
               listingPath: Option[String],
               description: String,
               apis: List[Endpoint],
               models: Map[String, Model]) {
  def model(name: String) = models.get(name)
  def toJValue = Api.toJValue(this)
}

object Api {
  import SwaggerSerializers._

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  private[this] implicit val formats = new DefaultFormats {
    override val dateFormat = new DateFormat {
      def format(d: JDate) = new DateTime(d).toString(Iso8601Date)
      def parse(s: String) = try {
        Option(Iso8601Date.parseDateTime(s).toDate)
      } catch {
        case _ ⇒ None
      }
    }
  } ++ Seq(
    new EnumNameSerializer(ParamType),
    new HttpMethodSerializer,
    new ParameterSerializer,
    new AllowableValuesSerializer,
    new ModelFieldSerializer) ++ JodaTimeSerializers.all

  def toJValue(doc: Any) = (Extraction.decompose(doc)(formats).noNulls)


}

private object SwaggerSerializers {
  import JsonDSL._
  class HttpMethodSerializer extends Serializer[HttpMethod] {
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), HttpMethod] = {
      case (TypeInfo(_, _), json) ⇒ null
    }
    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: HttpMethod ⇒ JString(x.toString)
    }
  }

  private[this] val simpleTypeList: List[String] = List("string", "number", "int", "boolean", "object", "Array", "null", "any")
  private[this] def listType(key: String, name: String, isUnique: Boolean): JValue = {
    val default = (key -> "Array") ~ ("uniqueItems" -> isUnique)
    val arrayType = name.substring(name.indexOf("[") + 1, name.indexOf("]"))
    if (simpleTypeList.contains(arrayType))
      default ~ ("items" -> (("type" -> arrayType): JValue))
    else
      default ~ ("items" -> (("$ref" -> arrayType): JValue))
  }

  private[this] def serializeDataType(key: String, dataType: DataType.DataType) = {
    dataType.name match {
      case n if n.toUpperCase(Locale.ENGLISH).startsWith("LIST[") => listType(key, n, isUnique = false)
      case n if n.toUpperCase(Locale.ENGLISH).startsWith("SET[") => listType(key, n, isUnique = true)
      case n => (key -> n): JValue
    }
  }


  class ParameterSerializer extends Serializer[Parameter] {

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, _root_.org.json4s.JValue), Parameter] = {
      case _ => null
    }

    def serialize(implicit format: Formats): PartialFunction[Any, _root_.org.json4s.JValue] = {
      case x: Parameter =>
        ("name" -> x.name) ~
        ("description" -> x.description) ~
        ("notes" -> x.notes) ~
        ("defaultValue" -> x.defaultValue) ~
        ("allowableValues" -> Extraction.decompose(x.allowableValues)) ~
        ("required" -> x.required) ~
        ("paramType" -> x.paramType.toString) ~
        ("allowMultiple" -> x.allowMultiple) merge serializeDataType("dataType", x.dataType)
    }
  }

  class ModelFieldSerializer extends Serializer[ModelField] {
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ModelField] = {
      case (TypeInfo(_, _), json) => null
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: ModelField => {
          val c = ("description" -> x.description) ~
          ("defaultValue" -> x.defaultValue) ~
          ("enum" -> x.enum) ~
          ("required" -> x.required)
        c merge serializeDataType("type", x.`type`)
      }
    }
  }


  class AllowableValuesSerializer extends Serializer[AllowableValues] {
    import AllowableValues._

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), AllowableValues] = {
      case (TypeInfo(_, _), json) ⇒ null
    }
    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case AnyValue ⇒ JNothing
      case AllowableValuesList(values)  ⇒ ("valueType" -> "LIST") ~ ("values" -> Extraction.decompose(values))
      case AllowableRangeValues(range)  ⇒ ("valueType" -> "RANGE") ~ ("min" -> range.start) ~ ("max" -> range.end)
    }
  }
}

object ParamType extends Enumeration {
  type ParamType = Value

  val Body = Value("body")
  val Query = Value("query")
  val Path = Value("path")
  val Header = Value("header")
}

object DataType {
  case class DataType(name: String)

  val Void = DataType("void")
  val String = DataType("string")
  val Int = DataType("int")
  val Boolean = DataType("boolean")
  val Date = DataType("date")
  val Enum = DataType("enum")
  val List = DataType("List")
  val Map = DataType("Map")
  val Tuple = DataType("tuple")

  object GenList {
    def apply(): DataType = List
    def apply(v: DataType): DataType = new DataType("List[%s]" format (v.name))
  }

  object GenMap {
    def apply(): DataType = Map
    def apply(k: DataType, v: DataType): DataType = new DataType("Map[%s, %s]" format(k.name, v.name))
  }

  def apply(name: String) = new DataType(name)
  def apply[T](implicit mf: Manifest[T]) = new DataType(mf.erasure.getSimpleName)
}

trait AllowableValues

object AllowableValues {
  case object AnyValue extends AllowableValues
  case class AllowableValuesList[T <% JValue](values: List[T]) extends AllowableValues
  case class AllowableRangeValues(values: Range) extends AllowableValues

  def apply(): AllowableValues = empty
  def apply[T <% JValue](values: T*): AllowableValues = apply(values.toList)
  def apply[T <% JValue](values: List[T]): AllowableValues = {
    AllowableValuesList(values)
  }
  def apply(values: Range): AllowableValues = AllowableRangeValues(values)
  def empty = AnyValue
}

case class Parameter(name: String,
                     description: String,
                     dataType: DataType.DataType,
                     notes: Option[String] = None,
                     paramType: ParamType.ParamType = ParamType.Query,
                     defaultValue: Option[String] = None,
                     allowableValues: AllowableValues = AllowableValues.AnyValue,
                     required: Boolean = true,
                     allowMultiple: Boolean = false)

case class ModelField(name: String,
                      description: String,
                      `type`: DataType.DataType,
                      defaultValue: Option[String] = None,
                      enum: List[String] = Nil,
                      required: Boolean = true)

object ModelField {
  implicit def modelField2tuple(m: ModelField) = (m.name, m)
}

case class Model(id: String,
                 description: String,
                 properties: Map[String, ModelField]) {

  def setRequired(property: String, required: Boolean) =
    copy(properties = (properties + (property -> properties(property).copy(required = required))))
}

object Model {
  implicit def model2tuple(m: Model) = (m.id, m)
}

case class Operation(httpMethod: HttpMethod,
                     responseClass: String,
                     summary: String,
                     notes: Option[String] = None,
                     deprecated: Boolean = false,
                     nickname: Option[String] = None,
                     parameters: List[Parameter] = Nil,
                     errorResponses: List[Error] = Nil)

case class Endpoint(path: String,
                    description: String,
                    secured: Boolean = true,
                    operations: List[Operation] = Nil)

case class Error(code: Int,
                 reason: String)
