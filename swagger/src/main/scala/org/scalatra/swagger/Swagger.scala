package org.scalatra
package swagger

import java.util.{ Date => JDate }
import net.liftweb.json._
import ext.{ JodaTimeSerializers, EnumNameSerializer }
import org.joda.time._
import format.ISODateTimeFormat

/**
 * An instance of this class is used to hold the API documentation.
 */
class Swagger(val swaggerVersion: String, val apiVersion: String) {
  private var _docs = Map.empty[String, Api]

  def docs = _docs.values

  /**
   * Returns the documentation for the given path.
   */
  def doc(path: String): Option[Api] = _docs.get(path)

  /**
   * Registers the documentation for an API with the given path.
   */
  def register(name: String, path: String, description: String, s: SwaggerSupport) = {
    _docs = _docs + (name -> Api(path, description, s.endpoints(path), s.models))
  }
}

case class Api(resourcePath: String,
               description: String,
               apis: List[Endpoint],
               models: Map[String, Model]) {
  def model(name: String) = models.get(name)
}

object Api {
  import JsonDSL._

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  implicit val formats = new DefaultFormats {
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
    new ModelFieldSerializer,
    new AllowableValuesSerializer,
    new DataTypeSerializer) ++ JodaTimeSerializers.all

  def toJObject(doc: Any) = (Extraction.decompose(doc)(formats) transform {
    case JField(_, JNull) | JField(_, JNothing) ⇒ JNothing
  }).asInstanceOf[JObject]

  class ModelFieldSerializer extends Serializer[HttpMethod] {
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), HttpMethod] = {
      case (TypeInfo(_, _), json) ⇒ null
    }
    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: HttpMethod ⇒ JString(x.toString)
    }
  }

  class DataTypeSerializer extends Serializer[DataType.DataType] {
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), DataType.DataType] = {
      case (TypeInfo(_, _), json) ⇒ null
    }
    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: DataType.DataType ⇒ JString(x.name)
    }
  }

  class AllowableValuesSerializer extends Serializer[AllowableValues] {
    import AllowableValues._

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), AllowableValues] = {
      case (TypeInfo(_, _), json) ⇒ null
    }
    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case AllowableValues.Any ⇒ JNothing
      case x: AllowableListValues  ⇒ ("valueType" -> "LIST") ~ ("values" -> x.values)
      case x: AllowableRangeValues  ⇒ ("valueType" -> "RANGE") ~ ("min" -> x.min) ~ ("max" -> x.max)
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
}

trait AllowableValues

object AllowableValues {
  case object Any extends AllowableValues
  case class AllowableListValues(values: List[String]) extends AllowableValues
  case class AllowableRangeValues(min: Int, max: Int) extends AllowableValues

  def apply(): AllowableValues = empty
  def apply(v: String): AllowableValues = {
    val pattern = "([A-Z]*)\\[(.*)\\]".r
    v match {
      case pattern(valueType, values) => {
        valueType match {
          case "LIST" => AllowableListValues(values.split(",").toList)
          case "RANGE" => {
            val r = values.split(",")
            AllowableRangeValues(r(0).toInt, r(1).toInt)
          }
        }
      }
      case _ => Any
    }
  }
  def empty = Any
}

case class Parameter(name: String,
                     description: String,
                     dataType: DataType.DataType,
                     notes: Option[String] = None,
                     paramType: ParamType.ParamType = ParamType.Query,
                     defaultValue: Option[String] = None,
                     allowableValues: AllowableValues = AllowableValues.Any,
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
    copy(properties = properties.updated(property, properties(property).copy(required = required)))
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
