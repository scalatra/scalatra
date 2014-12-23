package org.scalatra
package swagger

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.{ Date => JDate }

import grizzled.slf4j.Logger
import org.joda.time._
import org.joda.time.format.ISODateTimeFormat
import org.json4s._
import org.scalatra.swagger.reflect._
import org.scalatra.swagger.runtime.annotations.{ ApiModel, ApiModelProperty }

import scala.collection.JavaConverters._

trait SwaggerEngine[T <: SwaggerApi[_]] {
  def swaggerVersion: String
  def apiVersion: String
  def apiInfo: ApiInfo

  private[swagger] val _docs = new ConcurrentHashMap[String, T]().asScala

  private[this] var _authorizations = List.empty[AuthorizationType]
  def authorizations = _authorizations
  def addAuthorization(auth: AuthorizationType) { _authorizations ::= auth }

  def docs = _docs.values

  /**
   * Configurations used by UrlGenerator when creating baseUrl.
   */
  def baseUrlIncludeContextPath = true
  def baseUrlIncludeServletPath = false

  /**
   * Returns the documentation for the given path.
   */
  def doc(path: String): Option[T] = _docs.get(path)

  /**
   * Registers the documentation for an API with the given path.
   */
  def register(listingPath: String, resourcePath: String, description: Option[String], s: SwaggerSupportSyntax with SwaggerSupportBase, consumes: List[String], produces: List[String], protocols: List[String], authorizations: List[String])

}

object Swagger {

  val baseTypes = Set("byte", "boolean", "int", "long", "float", "double", "string", "date", "void", "Date", "DateTime", "DateMidnight", "Duration", "FiniteDuration", "Chronology")
  val excludes: Set[java.lang.reflect.Type] = Set(classOf[java.util.TimeZone], classOf[java.util.Date], classOf[DateTime], classOf[DateMidnight], classOf[ReadableInstant], classOf[Chronology], classOf[DateTimeZone])
  val containerTypes = Set("Array", "List", "Set")
  val SpecVersion = "1.2"
  val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  def collectModels[T: Manifest](alreadyKnown: Set[Model]): Set[Model] = collectModels(Reflector.scalaTypeOf[T], alreadyKnown)
  private[swagger] def collectModels(tpe: ScalaType, alreadyKnown: Set[Model], known: Set[ScalaType] = Set.empty): Set[Model] = {
    if (tpe.isMap) collectModels(tpe.typeArgs.head, alreadyKnown, tpe.typeArgs.toSet) ++ collectModels(tpe.typeArgs.last, alreadyKnown, tpe.typeArgs.toSet)
    else if (tpe.isCollection || tpe.isOption) {
      val ntpe = tpe.typeArgs.head
      if (!known.contains(ntpe)) collectModels(ntpe, alreadyKnown, known + ntpe)
      else Set.empty
    } else {
      if (alreadyKnown.map(_.id).contains(tpe.simpleName)) Set.empty
      else {
        val descr = Reflector.describe(tpe)
        descr match {
          case descriptor: ClassDescriptor =>
            val ctorModels = descriptor.mostComprehensive.filterNot(_.isPrimitive).toVector
            val propModels = descriptor.properties.filterNot(p => p.isPrimitive || ctorModels.exists(_.name == p.name))
            val subModels = (ctorModels.map(_.argType) ++ propModels.map(_.returnType)).toSet -- known
            val topLevel = for {
              tl <- subModels + descriptor.erasure
              if !(tl.isCollection || tl.isMap || tl.isOption)
              m <- modelToSwagger(tl)
            } yield m

            val nested = subModels.foldLeft((topLevel, known + descriptor.erasure)) { (acc, b) =>
              val m = collectModels(b, alreadyKnown, acc._2)
              (acc._1 ++ m, acc._2 + b)
            }
            nested._1
          case _ => Set.empty
        }
      }
    }
  }

  import org.scalatra.util.RicherString._
  def modelToSwagger[T](implicit mf: Manifest[T]): Option[Model] = modelToSwagger(Reflector.scalaTypeOf[T])

  private[this] def toModelProperty(descr: ClassDescriptor, position: Option[Int] = None, required: Boolean = true, description: Option[String] = None, allowableValues: String = "")(prop: PropertyDescriptor) = {
    val ctorParam = if (!prop.returnType.isOption) descr.mostComprehensive.find(_.name == prop.name) else None
    //    if (descr.simpleName == "Pet") println("converting property: " + prop)
    val mp = ModelProperty(
      DataType.fromScalaType(if (prop.returnType.isOption) prop.returnType.typeArgs.head else prop.returnType),
      if (position.isDefined && position.forall(_ >= 0)) position.get else ctorParam.map(_.argIndex).getOrElse(position.getOrElse(0)),
      required = required && !prop.returnType.isOption,
      description = description.flatMap(_.blankOption),
      allowableValues = convertToAllowableValues(allowableValues))
    //    if (descr.simpleName == "Pet") println("The property is: " + mp)
    prop.name -> mp
  }
  def modelToSwagger(klass: ScalaType): Option[Model] = {
    if (Reflector.isPrimitive(klass.erasure) || Reflector.isExcluded(klass.erasure, excludes.toSeq)) None
    else {
      val name = klass.simpleName

      val descr = Reflector.describe(klass).asInstanceOf[ClassDescriptor]
      val apiModel = Option(klass.erasure.getAnnotation(classOf[ApiModel]))

      val fields = klass.erasure.getDeclaredFields.toList collect {
        case f: Field if f.getAnnotation(classOf[ApiModelProperty]) != null =>
          val annot = f.getAnnotation(classOf[ApiModelProperty])
          val asModelProperty = toModelProperty(descr, Some(annot.position()), annot.required(), annot.description().blankOption, annot.allowableValues())_
          descr.properties.find(_.mangledName == f.getName) map asModelProperty

        case f: Field =>
          val asModelProperty = toModelProperty(descr)_
          descr.properties.find(_.mangledName == f.getName) map asModelProperty

      }

      val result = apiModel map { am =>
        Model(name, name, klass.fullName.blankOption, properties = fields.flatten, baseModel = am.parent.getName.blankOption, discriminator = am.discriminator.blankOption)
      } orElse Some(Model(name, name, klass.fullName.blankOption, properties = fields.flatten))
      //      if (descr.simpleName == "Pet") println("The collected fields:\n" + result)
      result
    }
  }

  private def convertToAllowableValues(csvString: String, paramType: String = null): AllowableValues = {
    if (csvString.toLowerCase.startsWith("range[")) {
      val ranges = csvString.substring(6, csvString.length() - 1).split(",")
      buildAllowableRangeValues(ranges, csvString, inclusive = true)
    } else if (csvString.toLowerCase.startsWith("rangeexclusive[")) {
      val ranges = csvString.substring(15, csvString.length() - 1).split(",")
      buildAllowableRangeValues(ranges, csvString, inclusive = false)
    } else {
      if (csvString.isBlank) {
        AllowableValues.AnyValue
      } else {
        val params = csvString.split(",").toList
        implicit val format = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)
        paramType match {
          case null => AllowableValues.AllowableValuesList(params)
          case "string" => AllowableValues.AllowableValuesList(params)
        }
      }
    }
  }

  private def buildAllowableRangeValues(ranges: Array[String], inputStr: String, inclusive: Boolean = true): AllowableValues.AllowableRangeValues = {
    var min: java.lang.Float = 0
    var max: java.lang.Float = 0
    if (ranges.size < 2) {
      throw new RuntimeException("Allowable values format " + inputStr + "is incorrect")
    }
    if (ranges(0).equalsIgnoreCase("Infinity")) {
      min = Float.PositiveInfinity
    } else if (ranges(0).equalsIgnoreCase("-Infinity")) {
      min = Float.NegativeInfinity
    } else {
      min = ranges(0).toFloat
    }
    if (ranges(1).equalsIgnoreCase("Infinity")) {
      max = Float.PositiveInfinity
    } else if (ranges(1).equalsIgnoreCase("-Infinity")) {
      max = Float.NegativeInfinity
    } else {
      max = ranges(1).toFloat
    }
    val allowableValues =
      AllowableValues.AllowableRangeValues(if (inclusive) Range.inclusive(min.toInt, max.toInt) else Range(min.toInt, max.toInt))
    allowableValues
  }

}

/**
 * An instance of this class is used to hold the API documentation.
 */
class Swagger(val swaggerVersion: String, val apiVersion: String, val apiInfo: ApiInfo) extends SwaggerEngine[Api] {
  private[this] val logger = Logger[this.type]

  /**
   * Registers the documentation for an API with the given path.
   */
  def register(listingPath: String, resourcePath: String, description: Option[String], s: SwaggerSupportSyntax with SwaggerSupportBase, consumes: List[String], produces: List[String], protocols: List[String], authorizations: List[String]) = {
    logger.debug(s"registering swagger api with: { listingPath: $listingPath, resourcePath: $resourcePath, description: $resourcePath, servlet: ${s.getClass} }")
    val endpoints: List[Endpoint] = s.endpoints(resourcePath) collect { case m: Endpoint => m }
    _docs += listingPath -> Api(
      apiVersion,
      swaggerVersion,
      resourcePath,
      description,
      (produces ::: endpoints.flatMap(_.operations.flatMap(_.produces))).distinct,
      (consumes ::: endpoints.flatMap(_.operations.flatMap(_.consumes))).distinct,
      (protocols ::: endpoints.flatMap(_.operations.flatMap(_.protocols))).distinct,
      endpoints,
      s.models.toMap,
      (authorizations ::: endpoints.flatMap(_.operations.flatMap(_.authorizations))).distinct,
      0)
  }
}

trait SwaggerApi[T <: SwaggerEndpoint[_]] {

  def apiVersion: String
  def swaggerVersion: String
  def resourcePath: String
  def description: Option[String]
  def produces: List[String]
  def consumes: List[String]
  def protocols: List[String]
  def authorizations: List[String]
  def position: Int
  def apis: List[T]
  def models: Map[String, Model]

  def model(name: String) = models.get(name)
}

case class ResourceListing(
  apiVersion: String,
  swaggerVersion: String = Swagger.SpecVersion,
  apis: List[ApiListingReference] = Nil,
  authorizations: List[AuthorizationType] = Nil,
  info: Option[ApiInfo] = None)

case class ApiListingReference(path: String, description: Option[String] = None, position: Int = 0)

case class Api(apiVersion: String,
    swaggerVersion: String,
    resourcePath: String,
    description: Option[String] = None,
    produces: List[String] = Nil,
    consumes: List[String] = Nil,
    protocols: List[String] = Nil,
    apis: List[Endpoint] = Nil,
    models: Map[String, Model] = Map.empty,
    authorizations: List[String] = Nil,
    position: Int = 0) extends SwaggerApi[Endpoint] {
}

object ParamType extends Enumeration {
  type ParamType = Value

  /** A parameter carried in a POST body. **/
  val Body = Value("body")

  /**
   * A parameter carried on the query string.
   *
   * E.g. http://example.com/foo?param=2
   */
  val Query = Value("query")

  /**
   * A path parameter mapped to a Scalatra route.
   *
   * E.g. http://example.com/foo/2 where there's a route like
   * get("/foo/:id").
   */
  val Path = Value("path")

  /** A parameter carried in an HTTP header. **/
  val Header = Value("header")

  val File = Value("file")

  val Form = Value("form")
}

sealed trait DataType {
  def name: String
}
object DataType {

  case class ValueDataType(name: String, format: Option[String] = None, qualifiedName: Option[String] = None) extends DataType
  case class ContainerDataType(name: String, typeArg: Option[DataType] = None, uniqueItems: Boolean = false) extends DataType

  val Void = DataType("void")
  val String = DataType("string")
  val Byte = DataType("string", Some("byte"))
  val Int = DataType("integer", Some("int32"))
  val Long = DataType("integer", Some("int64"))
  val Float = DataType("number", Some("float"))
  val Double = DataType("number", Some("double"))
  val Boolean = DataType("boolean")
  val Date = DataType("string", Some("date"))
  val DateTime = DataType("string", Some("date-time"))

  object GenList {
    def apply(): DataType = ContainerDataType("List")
    def apply(v: DataType): DataType = new ContainerDataType("List", Some(v))
  }

  object GenSet {
    def apply(): DataType = ContainerDataType("Set", uniqueItems = true)
    def apply(v: DataType): DataType = new ContainerDataType("Set", Some(v), uniqueItems = true)
  }

  object GenArray {
    def apply(): DataType = ContainerDataType("Array")
    def apply(v: DataType): DataType = new ContainerDataType("Array", Some(v))
  }

  //  object GenMap {
  //    def apply(): DataType = Map
  //    def apply(k: DataType, v: DataType): DataType = new DataType("Map[%s, %s]" format(k.name, v.name))
  //  }
  //
  def apply(name: String, format: Option[String] = None, qualifiedName: Option[String] = None) =
    new ValueDataType(name, format, qualifiedName)
  def apply[T](implicit mf: Manifest[T]): DataType = fromManifest[T](mf)

  private[this] val StringTypes = Set[Class[_]](classOf[String], classOf[java.lang.String])
  private[this] def isString(klass: Class[_]) = StringTypes contains klass
  private[this] val BoolTypes = Set[Class[_]](classOf[Boolean], classOf[java.lang.Boolean])
  private[this] def isBool(klass: Class[_]) = BoolTypes contains klass

  private[swagger] def fromManifest[T](implicit mf: Manifest[T]): DataType = {
    fromScalaType(Reflector.scalaTypeOf[T])
  }
  private[swagger] def fromClass(klass: Class[_]): DataType = fromScalaType(Reflector.scalaTypeOf(klass))
  private[swagger] def fromScalaType(st: ScalaType): DataType = {
    val klass = if (st.isOption && st.typeArgs.size > 0) st.typeArgs.head.erasure else st.erasure
    if (classOf[Unit].isAssignableFrom(klass) || classOf[Void].isAssignableFrom(klass)) this.Void
    else if (isString(klass)) this.String
    else if (classOf[Byte].isAssignableFrom(klass) || classOf[java.lang.Byte].isAssignableFrom(klass)) this.Byte
    else if (classOf[Long].isAssignableFrom(klass) || classOf[java.lang.Long].isAssignableFrom(klass)) this.Long
    else if (isInt(klass)) this.Int
    else if (classOf[Float].isAssignableFrom(klass) || classOf[java.lang.Float].isAssignableFrom(klass)) this.Float
    else if (isDecimal(klass)) this.Double
    else if (isDate(klass)) this.Date
    else if (isDateTime(klass)) this.DateTime
    else if (isBool(klass)) this.Boolean
    //    else if (classOf[java.lang.Enum[_]].isAssignableFrom(klass)) this.Enum
    //    else if (isMap(klass)) {
    //      if (st.typeArgs.size == 2) {
    //        val (k :: v :: Nil) = st.typeArgs.toList
    //        GenMap(fromScalaType(k), fromScalaType(v))
    //      } else GenMap()
    //    }
    else if (classOf[scala.collection.Set[_]].isAssignableFrom(klass) || classOf[java.util.Set[_]].isAssignableFrom(klass)) {
      if (st.typeArgs.nonEmpty) GenSet(fromScalaType(st.typeArgs.head))
      else GenSet()
    } else if (classOf[collection.Seq[_]].isAssignableFrom(klass) || classOf[java.util.List[_]].isAssignableFrom(klass)) {
      if (st.typeArgs.nonEmpty) GenList(fromScalaType(st.typeArgs.head))
      else GenList()
    } else if (st.isArray || isCollection(klass)) {
      if (st.typeArgs.nonEmpty) GenArray(fromScalaType(st.typeArgs.head))
      else GenArray()
    } else {
      val stt = if (st.isOption) st.typeArgs.head else st
      new ValueDataType(stt.simpleName, qualifiedName = Option(stt.fullName))
    }
  }

  private[this] val IntTypes =
    Set[Class[_]](classOf[Int], classOf[java.lang.Integer], classOf[Short], classOf[java.lang.Short], classOf[BigInt], classOf[java.math.BigInteger])
  private[this] def isInt(klass: Class[_]) = IntTypes.contains(klass)

  private[this] val DecimalTypes =
    Set[Class[_]](classOf[Double], classOf[java.lang.Double], classOf[BigDecimal], classOf[java.math.BigDecimal])
  private[this] def isDecimal(klass: Class[_]) = DecimalTypes contains klass

  private[this] val DateTypes =
    Set[Class[_]](classOf[DateMidnight])
  private[this] def isDate(klass: Class[_]) = DateTypes.exists(_.isAssignableFrom(klass))
  private[this] val DateTimeTypes =
    Set[Class[_]](classOf[JDate], classOf[DateTime])
  private[this] def isDateTime(klass: Class[_]) = DateTimeTypes.exists(_.isAssignableFrom(klass))
  //
  //  private[this] def isMap(klass: Class[_]) =
  //    classOf[collection.Map[_, _]].isAssignableFrom(klass) ||
  //    classOf[java.util.Map[_, _]].isAssignableFrom(klass)

  private[this] def isCollection(klass: Class[_]) =
    classOf[collection.Traversable[_]].isAssignableFrom(klass) ||
      classOf[java.util.Collection[_]].isAssignableFrom(klass)

}

case class ApiInfo(
  title: String,
  description: String,
  termsOfServiceUrl: String,
  contact: String,
  license: String,
  licenseUrl: String)

trait AllowableValues

object AllowableValues {
  case object AnyValue extends AllowableValues
  case class AllowableValuesList[T](values: List[T]) extends AllowableValues
  case class AllowableRangeValues(values: Range) extends AllowableValues

  def apply(): AllowableValues = empty
  def apply[T](values: T*): AllowableValues = apply(values.toList)
  def apply[T](values: List[T]): AllowableValues = AllowableValuesList(values)
  def apply(values: Range): AllowableValues = AllowableRangeValues(values)
  def empty = AnyValue
}

case class Parameter(name: String,
  `type`: DataType,
  description: Option[String] = None,
  notes: Option[String] = None,
  paramType: ParamType.ParamType = ParamType.Query,
  defaultValue: Option[String] = None,
  allowableValues: AllowableValues = AllowableValues.AnyValue,
  required: Boolean = true,
  //                     allowMultiple: Boolean = false,
  paramAccess: Option[String] = None,
  position: Int = 0)

case class ModelProperty(`type`: DataType,
  position: Int = 0,
  required: Boolean = false,
  description: Option[String] = None,
  allowableValues: AllowableValues = AllowableValues.AnyValue,
  items: Option[ModelRef] = None)

case class Model(id: String,
    name: String,
    qualifiedName: Option[String] = None,
    description: Option[String] = None,
    properties: List[(String, ModelProperty)] = Nil,
    baseModel: Option[String] = None,
    discriminator: Option[String] = None) {

  def setRequired(property: String, required: Boolean): Model = {
    val prop = properties.find(_._1 == property).get
    copy(properties = (property -> prop._2.copy(required = required)) :: properties)
  }
}

case class ModelRef(
  `type`: String,
  ref: Option[String] = None,
  qualifiedType: Option[String] = None)

case class LoginEndpoint(url: String)
case class TokenRequestEndpoint(url: String, clientIdName: String, clientSecretName: String)
case class TokenEndpoint(url: String, tokenName: String)

trait AuthorizationType {
  def `type`: String
}
case class OAuth(
    scopes: List[String],
    grantTypes: List[GrantType]) extends AuthorizationType {
  override val `type` = "oauth2"
}
case class ApiKey(keyname: String, passAs: String = "header") extends AuthorizationType {
  override val `type` = "apiKey"
}

trait GrantType {
  def `type`: String
}
case class ImplicitGrant(
    loginEndpoint: LoginEndpoint,
    tokenName: String) extends GrantType {
  def `type` = "implicit"
}
case class AuthorizationCodeGrant(
    tokenRequestEndpoint: TokenRequestEndpoint,
    tokenEndpoint: TokenEndpoint) extends GrantType {
  def `type` = "authorization_code"
}
trait SwaggerOperation {
  @deprecated("Swagger spec 1.2 renamed `httpMethod` to `method`.", "2.2.2")
  def httpMethod: HttpMethod = method
  def method: HttpMethod
  def responseClass: DataType
  def summary: String
  def notes: Option[String]
  def deprecated: Boolean
  def nickname: Option[String]
  def produces: List[String]
  def consumes: List[String]
  def protocols: List[String]
  def authorizations: List[String]
  def parameters: List[Parameter]
  @deprecated("Swagger spec 1.2 renamed `errorResponses` to `responseMessages`.", "2.2.2")
  def errorResponses: List[ResponseMessage[_]] = responseMessages
  def responseMessages: List[ResponseMessage[_]]
  //  def supportedContentTypes: List[String]
  def position: Int
}
case class Operation(method: HttpMethod,
  responseClass: DataType,
  summary: String,
  position: Int,
  notes: Option[String] = None,
  deprecated: Boolean = false,
  nickname: Option[String] = None,
  parameters: List[Parameter] = Nil,
  responseMessages: List[ResponseMessage[_]] = Nil,
  //                     supportedContentTypes: List[String] = Nil,
  consumes: List[String] = Nil,
  produces: List[String] = Nil,
  protocols: List[String] = Nil,
  authorizations: List[String] = Nil) extends SwaggerOperation

trait SwaggerEndpoint[T <: SwaggerOperation] {
  def path: String
  def description: Option[String]
  def operations: List[T]
}

case class Endpoint(path: String,
  description: Option[String] = None,
  operations: List[Operation] = Nil) extends SwaggerEndpoint[Operation]

trait ResponseMessage[T] {
  def code: Int
  def message: T
}
case class StringResponseMessage(code: Int, message: String) extends ResponseMessage[String]
