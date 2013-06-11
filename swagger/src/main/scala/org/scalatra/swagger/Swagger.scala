package org.scalatra
package swagger

import java.util.{Date => JDate, Locale}
import org.json4s._
import ext.{ JodaTimeSerializers, EnumNameSerializer }
import org.joda.time._
import format.ISODateTimeFormat
import grizzled.slf4j.Logger
import java.util.Date
import reflect.{ScalaType, PrimitiveDescriptor, ClassDescriptor, Reflector}
import com.wordnik.swagger.core.{DocumentationAllowableValues, DocumentationAllowableRangeValues, DocumentationAllowableListValues, ApiPropertiesReader}
import collection.JavaConverters._
import org.scalatra.swagger.AllowableValues.AllowableValuesList
import org.scalatra.swagger.AllowableValues.AllowableRangeValues


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

object Swagger {
  def collectModels[T: Manifest](alreadyKnown: Set[Model]): Set[Model] = collectModels(Reflector.scalaTypeOf[T], alreadyKnown)
  private[swagger] def collectModels(tpe: ScalaType, alreadyKnown: Set[Model], known: Set[ScalaType] = Set.empty): Set[Model] = {
    if (tpe.isMap) collectModels(tpe.typeArgs.head, alreadyKnown, tpe.typeArgs.toSet) ++ collectModels(tpe.typeArgs.last, alreadyKnown, tpe.typeArgs.toSet)
    else if (tpe.isCollection || tpe.isOption) {
      val ntpe = tpe.typeArgs.head
      if (! known.contains(ntpe)) collectModels(ntpe, alreadyKnown, known + ntpe)
      else Set.empty
    }
    else {
      if (alreadyKnown.map(_.id).contains(tpe.simpleName)) Set.empty
      else {
        val descr = Reflector.describe(tpe)
        descr match {
          case descriptor: ClassDescriptor =>
            val ctorModels = descriptor.mostComprehensive.filterNot(_.isPrimitive)
            val propModels = descriptor.properties.filterNot(p => p.isPrimitive || ctorModels.exists(_.name == p.name))
            val subModels = Set((ctorModels.map(_.argType) ++ propModels.map(_.returnType)):_*)
            val topLevel = for {
              tl <- (subModels + descriptor.erasure)
              if !(tl.isCollection || tl.isOption || tl.isMap)
              m <- modelToSwagger(tl.erasure)
            } yield m

            val nested = subModels.foldLeft((topLevel, known + descriptor.erasure)){ (acc, b) =>
              val m = collectModels(b, alreadyKnown, acc._2)
              (acc._1 ++ m, acc._2 + b)
            }
            nested._1
          case _ => Set.empty
        }
      }
    }
  }

  def modelToSwagger[T](implicit mf: Manifest[T]): Option[Model] = {
    modelToSwagger(mf.erasure)
  }
  def modelToSwagger(klass: Class[_]): Option[Model] = {
    if (Reflector.isPrimitive(klass) || Reflector.isExcluded(klass)) None
    else {
      val docObj = ApiPropertiesReader.read(klass)
      val name = docObj.getName
      val flds = docObj.getFields.asScala.filter(d => d.paramType != null)

      val fields = for (field <- flds) yield {
        (field.name -> 
          ModelField(field.name, 
                     field.description, 
                     DataType(field.paramType), 
                     allowableValues = allowableValuesToString(field.allowableValues))) }

      Some(Model(name, name, fields.toMap))
    }
  }
  private def allowableValuesToString(allowableValues: DocumentationAllowableValues) = {
    import scala.collection.JavaConversions._

    allowableValues match {
      case list:DocumentationAllowableListValues => AllowableValuesList(list.getValues.toList)
      case range:DocumentationAllowableRangeValues => AllowableRangeValues(Range(range.getMin.toInt, range.getMax.toInt))
      case _ => AllowableValues.AnyValue
    }
  }
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
    val endpoints: List[Endpoint] = s.endpoints(path) collect { case m: Endpoint => m }
    _docs = _docs + (name -> Api(path, listingPath, description, endpoints, s.models.toMap))
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

  private[swagger] implicit val formats = new DefaultFormats {
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

  def toJValue(doc: Any) = (Extraction.decompose(doc)(formats))


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
          ("required" -> x.required) ~
          ("allowableValues" -> Extraction.decompose(x.allowableValues))
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

  val File = Value("file")
}

object DataType {
  case class DataType(name: String)

  val Void = DataType("void")
  val String = DataType("string")
  val Int = DataType("int")
  val Long = DataType("long")
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

  object GenSet {
    def apply(): DataType = DataType("Set")
    def apply(v: DataType): DataType = new DataType("Set[%s]" format (v.name))
  }

  object GenArray {
    def apply(): DataType = DataType("Array")
    def apply(v: DataType): DataType = new DataType("Array[%s]" format (v.name))
  }

  object GenMap {
    def apply(): DataType = Map
    def apply(k: DataType, v: DataType): DataType = new DataType("Map[%s, %s]" format(k.name, v.name))
  }

  def apply(name: String) = new DataType(name)
  def apply[T](implicit mf: Manifest[T]): DataType = fromManifest[T](mf)

  private[this] val StringTypes = Set[Class[_]](classOf[String],classOf[java.lang.String])
  private[this] val BoolTypes = Set[Class[_]](classOf[Boolean],classOf[java.lang.Boolean])

  private[swagger] def fromManifest[T](implicit mf: Manifest[T]): DataType = {
    fromScalaType(Reflector.scalaTypeOf[T])
  }
  private[swagger] def fromClass(klass: Class[_]): DataType = fromScalaType(Reflector.scalaTypeOf(klass))
  private[swagger] def fromScalaType(st: ScalaType): DataType = {
    val klass = st.erasure
    if (classOf[Unit].isAssignableFrom(klass)) this.Void
    else if (StringTypes.contains(klass)) this.String
    else if (classOf[Byte].isAssignableFrom(klass) || classOf[java.lang.Byte].isAssignableFrom(klass)) DataType("byte")
    else if (classOf[Long].isAssignableFrom(klass) || classOf[java.lang.Long].isAssignableFrom(klass)) DataType("long")
    else if (isInt(klass)) this.Int
    else if (classOf[Float].isAssignableFrom(klass) || classOf[java.lang.Float].isAssignableFrom(klass)) DataType("float")
    else if (isDecimal(klass)) DataType("double")
    else if (isDate(klass)) this.Date
    else if (BoolTypes contains klass) this.Boolean
    else if (classOf[java.lang.Enum[_]].isAssignableFrom(klass)) this.Enum
    else if (isMap(klass)) {
      if (st.typeArgs.size == 2) {
        val (k :: v :: Nil) = st.typeArgs.toList
        GenMap(fromScalaType(k), fromScalaType(v))
      } else GenMap()
    }
    else if (classOf[scala.collection.Set[_]].isAssignableFrom(klass) || classOf[java.util.Set[_]].isAssignableFrom(klass)) {
      if (st.typeArgs.nonEmpty) GenSet(fromScalaType(st.typeArgs.head))
      else GenSet()
    }
    else if (classOf[collection.Seq[_]].isAssignableFrom(klass) || classOf[java.util.List[_]].isAssignableFrom(klass)) {
      if (st.typeArgs.nonEmpty) GenList(fromScalaType(st.typeArgs.head))
      else GenList()
    }
    else if (st.isArray || isCollection(klass)) {
      if (st.typeArgs.nonEmpty) GenArray(fromScalaType(st.typeArgs.head))
      else GenArray()
    }
    else new DataType(klass.getSimpleName())
  }

  private[this] val IntTypes =
    Set[Class[_]](classOf[Int], classOf[java.lang.Integer], classOf[Short], classOf[java.lang.Short], classOf[BigInt], classOf[java.math.BigInteger])
  private[this] def isInt(klass: Class[_]) = IntTypes.contains(klass)

  private[this] val DecimalTypes =
    Set[Class[_]](classOf[Double], classOf[java.lang.Double], classOf[BigDecimal], classOf[java.math.BigDecimal])
  private[this] def isDecimal(klass: Class[_]) = DecimalTypes contains klass

  private[this] val DateTypes =
    Set[Class[_]](classOf[Date], classOf[DateTime])
  private[this] def isDate(klass: Class[_]) = DateTypes.exists(_.isAssignableFrom(klass))
  
  private[this] def isMap(klass: Class[_]) =
    classOf[collection.Map[_, _]].isAssignableFrom(klass) ||
    classOf[java.util.Map[_, _]].isAssignableFrom(klass)

  private[this] def isCollection(klass: Class[_]) =
    classOf[collection.Traversable[_]].isAssignableFrom(klass) ||
    classOf[java.util.Collection[_]].isAssignableFrom(klass)

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
                      allowableValues: AllowableValues = AllowableValues.AnyValue,
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
  def supportedContentTypes: List[String]
}
case class Operation(httpMethod: HttpMethod,
                     responseClass: String,
                     summary: String,
                     notes: Option[String] = None,
                     deprecated: Boolean = false,
                     nickname: Option[String] = None,
                     parameters: List[Parameter] = Nil,
                     errorResponses: List[Error] = Nil,
                     supportedContentTypes: List[String]) extends SwaggerOperation
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
