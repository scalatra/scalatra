package org.scalatra
package swagger

import java.util.{Date => JDate, Locale}
import org.json4s._
import ext.{ JodaTimeSerializers, EnumNameSerializer }
import org.joda.time._
import format.ISODateTimeFormat
import grizzled.slf4j.Logger
import java.math.BigInteger
import java.util.Date

trait SwaggerEngine[T <: SwaggerApi[_]] {
  def swaggerVersion: String 
  def apiVersion: String
  
  
  private[swagger] var _docs = Map.empty[String, T]

  def docs = _docs.values

  /**
   * Returns the documentation for the given path.
   */
  def doc(path: String): Option[T] = _docs.get(path)

    /**
   * Registers the documentation for an API with the given path.
   */
  def register(name: String, path: String, description: String, s: SwaggerSupportSyntax with SwaggerSupportBase, listingPath: Option[String])

}

/**
 * An instance of this class is used to hold the API documentation.
 */
class Swagger(val swaggerVersion: String, val apiVersion: String) extends SwaggerEngine[Api] {
  private[this] val logger = Logger[this.type]
  /**
   * Registers the documentation for an API with the given path.
   */
  def register(name: String, path: String, description: String, s: SwaggerSupportSyntax with SwaggerSupportBase, listingPath: Option[String] = None) = {
    logger.debug("registering swagger api with: { name: %s, path: %s, description: %s, servlet: %s, listingPath: %s }" format (name, path, description, s.getClass, listingPath))
    val endpoints: List[Endpoint] = s.endpoints(path).foldLeft(List.empty[Endpoint]) { (acc, a) =>
      a match {
        case m: Endpoint => m :: acc
        case _ => acc
      }
    }
    _docs = _docs + (name -> Api(path, listingPath, description, endpoints, s.models))
  }
}

trait SwaggerApi[T <: SwaggerEndpoint[_]] {
  def resourcePath: String
  def listingPath: Option[String]
  def description: String
  def apis: List[T]
  def models: Map[String, Model]
  
  def model(name: String) = models.get(name)
}

case class Api(resourcePath: String,
               listingPath: Option[String],
               description: String,
               apis: List[Endpoint],
               models: Map[String, Model]) extends SwaggerApi[Endpoint] {
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

private[swagger] object SwaggerSerializers {
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

  /** A parameter carried in a POST body. **/
  val Body = Value("body")

  /** A parameter carried on the query string. 
    *  
    * E.g. http://example.com/foo?param=2 
    */
  val Query = Value("query")

  /** A path parameter mapped to a Scalatra route.
    *
    * E.g. http://example.com/foo/2 where there's a route like
    * get("/foo/:id").
    */
  val Path = Value("path")

  /** A parameter carried in an HTTP header. **/
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
  def apply[T](implicit mf: Manifest[T]): DataType = fromManifest[T](mf)

  private[this] def fromManifest[T](implicit mf: Manifest[T]): DataType = {
    if (mf <:< manifest[Unit]) this.Void
    else if (mf <:< manifest[String] || mf <:< manifest[java.lang.String]) this.String
    else if (isInt[T]) this.Int
    else if (isDecimal[T]) DataType("double")
    else if (isDate[T]) this.Date
    else if (mf <:< manifest[Boolean] || mf <:< manifest[java.lang.Boolean]) this.Boolean
    else if (mf <:< manifest[java.lang.Enum[_]]) this.Enum
    else if (isMap[T]) {
      if (mf.typeArguments.size == 2) {
        val (k :: v :: Nil) = mf.typeArguments
        GenMap(fromManifest(k), fromManifest(v))
      } else GenMap()
    }
    else if (isCollection[T]) {
      if (mf.typeArguments.size > 0)
        GenList(fromManifest(mf.typeArguments.head))
      else
        GenList()
    }
    else new DataType(mf.erasure.getSimpleName())
  }
  
  private[this] def isInt[T](implicit mf: Manifest[T]) = 
    mf <:< manifest[Int] || 
    mf <:< manifest[java.lang.Integer] || 
    mf <:< manifest[Long] || 
    mf <:< manifest[java.lang.Long] || 
    mf <:< manifest[BigInt] || 
    mf <:< manifest[java.math.BigInteger]
  
  private[this] def isDecimal[T](implicit mf: Manifest[T]) = 
    mf <:< manifest[Double] ||
    mf <:< manifest[java.lang.Double] ||
    mf <:< manifest[Float] ||
    mf <:< manifest[java.lang.Float] ||
    mf <:< manifest[BigDecimal] ||
    mf <:< manifest[java.math.BigDecimal]
  
  private[this] def isDate[T](implicit mf: Manifest[T]) = mf <:< manifest[Date] || mf <:< manifest[DateTime]
  
  private[this] def isMap[T](implicit mf: Manifest[T]) =
    classOf[collection.Map[_, _]].isAssignableFrom(mf.erasure) ||
    classOf[java.util.Map[_, _]].isAssignableFrom(mf.erasure)

  private[this] def isCollection[T](implicit mf: Manifest[T]) =
    classOf[collection.Traversable[_]].isAssignableFrom(mf.erasure) ||
    classOf[java.util.Collection[_]].isAssignableFrom(mf.erasure)

}

trait AllowableValues

object AllowableValues {
  case object AnyValue extends AllowableValues
  case class AllowableValuesList[T](values: List[T]) extends AllowableValues
  case class AllowableRangeValues(values: Range) extends AllowableValues

  def apply(): AllowableValues = empty
  def apply[T](values: T*): AllowableValues = apply(values.toList)
  def apply[T](values: List[T]): AllowableValues = {
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
trait SwaggerOperation {
  def httpMethod: HttpMethod
  def responseClass: String
  def summary: String
  def notes: Option[String]
  def deprecated: Boolean
  def nickname: Option[String]
  def parameters: List[Parameter]
  def errorResponses: List[Error]
}
case class Operation(httpMethod: HttpMethod,
                     responseClass: String,
                     summary: String,
                     notes: Option[String] = None,
                     deprecated: Boolean = false,
                     nickname: Option[String] = None,
                     parameters: List[Parameter] = Nil,
                     errorResponses: List[Error] = Nil) extends SwaggerOperation
trait SwaggerEndpoint[T <: SwaggerOperation] {
  def path: String
  def description: String
  def secured: Boolean
  def operations: List[T]
}
case class Endpoint(path: String,
                    description: String,
                    secured: Boolean = false,
                    operations: List[Operation] = Nil) extends SwaggerEndpoint[Operation]

case class Error(code: Int,
                 reason: String)
