package org.scalatra
package swagger

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.Date as JDate

import org.json4s.JsonAST.JValue
import org.scalatra.swagger.reflect.*
import org.scalatra.swagger.runtime.annotations.{ApiModel, ApiModelProperty}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.*

trait SwaggerEngine {
  def swaggerVersion: String
  def apiVersion: String
  def host: String
  def apiInfo: ApiInfo
  def extraSwaggerDefinition: Option[JValue]

  private[swagger] val _docs = new ConcurrentHashMap[String, Api]().asScala

  private[this] var _authorizations                   = List.empty[AuthorizationType]
  def authorizations                                  = _authorizations
  def addAuthorization(auth: AuthorizationType): Unit = { _authorizations ::= auth }

  def docs = _docs.values

  /** Configurations used by UrlGenerator when creating baseUrl.
    */
  def baseUrlIncludeContextPath = true
  def baseUrlIncludeServletPath = false

  /** Returns the documentation for the given path.
    */
  def doc(path: String): Option[Api] = _docs.get(path)

  /** Registers the documentation for an API with the given path.
    */
  def register(
      listingPath: String,
      resourcePath: String,
      description: Option[String],
      s: SwaggerSupportSyntax & SwaggerSupportBase,
      consumes: List[String],
      produces: List[String],
      protocols: List[String],
      authorizations: List[String]
  ): Unit

}

object Swagger {

  val excludes: Set[java.lang.reflect.Type] = Set(
    classOf[java.util.TimeZone],
    classOf[java.util.Date],
    classOf[java.time.OffsetDateTime],
    classOf[java.time.ZonedDateTime],
    classOf[java.time.LocalDateTime],
    classOf[java.time.LocalDate],
    classOf[java.time.LocalTime],
    classOf[java.time.Instant],
    classOf[java.time.chrono.Chronology],
    classOf[java.time.ZoneOffset]
  )
  val SpecVersion = "2.0"

  def collectModels[T: Manifest](alreadyKnown: Set[Model]): Set[Model] =
    collectModels(Reflector.scalaTypeOf[T], alreadyKnown)
  private[swagger] def collectModels(
      tpe: ScalaType,
      alreadyKnown: Set[Model],
      known: Set[ScalaType] = Set.empty
  ): Set[Model] = {

    if (excludes contains tpe.erasure) {
      // Date classes only output 'date-time' in a uniform format, so there is no need to analyze the structure of the object.
      // Therefore, the analysis of classes specified as excludes will be skipped.
      Set.empty
    } else if (tpe.isMap) {
      collectModels(tpe.typeArgs.head, alreadyKnown, tpe.typeArgs.toSet) ++ collectModels(
        tpe.typeArgs.last,
        alreadyKnown,
        tpe.typeArgs.toSet
      )

    } else if ((tpe.isCollection && tpe.typeArgs.nonEmpty) || (tpe.isOption && tpe.typeArgs.nonEmpty)) {
      val ntpe = tpe.typeArgs.head
      if (!known.contains(ntpe)) {
        collectModels(ntpe, alreadyKnown, known + ntpe)
      } else {
        Set.empty
      }

    } else {
      if (alreadyKnown.map(_.id).contains(tpe.simpleName)) {
        Set.empty
      } else {
        val descriptor = Reflector.describe(tpe)
        descriptor match {
          case descriptor: ClassDescriptor =>
            val ctorModels = descriptor.mostComprehensive.filterNot(_.isPrimitive).toVector
            val propModels = descriptor.properties.filterNot(p => p.isPrimitive || ctorModels.exists(_.name == p.name))
            val subModels  = (ctorModels.map(_.argType) ++ propModels.map(_.returnType)).toSet -- known
            val topLevel   = for {
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

  import org.scalatra.util.RicherString.*
  def modelToSwagger[T](implicit mf: Manifest[T]): Option[Model] = modelToSwagger(Reflector.scalaTypeOf[T])

  private[this] def toModelProperty(
      descriptor: ClassDescriptor,
      position: Option[Int] = None,
      required: Boolean = true,
      description: Option[String] = None,
      allowableValues: String = "",
      example: Option[String] = None,
      minimumValue: Option[Double] = None,
      maximumValue: Option[Double] = None,
      default: Option[String] = None,
      hidden: Boolean = false
  )(prop: PropertyDescriptor) = {
    val ctorParam = descriptor.mostComprehensive.find(_.name == prop.name)
    val mp        = ModelProperty(
      `type` = DataType.fromScalaType(
        if (prop.returnType.isOption) ctorParam.map(_.argType.typeArgs.head).getOrElse(prop.returnType.typeArgs.head)
        else ctorParam.map(_.argType).getOrElse(prop.returnType)
      ),
      position =
        if (position.isDefined && position.forall(_ >= 0)) position.get
        else ctorParam.map(_.argIndex).getOrElse(position.getOrElse(0)),
      required = required && !prop.returnType.isOption,
      description = description.flatMap(_.blankOption),
      allowableValues = convertToAllowableValues(allowableValues),
      example = example.flatMap(_.blankOption),
      default = default.flatMap(_.blankOption),
      minimumValue = minimumValue,
      maximumValue = maximumValue,
      hidden = hidden
    )
    prop.name -> mp
  }
  def modelToSwagger(klass: ScalaType): Option[Model] = {
    if (Reflector.isPrimitive(klass.erasure) || Reflector.isExcluded(klass.erasure, excludes.toSeq)) None
    else {
      val name = klass.simpleName

      val descriptor = Reflector.describe(klass).asInstanceOf[ClassDescriptor]
      val apiModel   = Option(klass.erasure.getAnnotation(classOf[ApiModel]))

      val fields = klass.erasure.getDeclaredFields.toList collect {
        case f: Field if f.getAnnotation(classOf[ApiModelProperty]) != null =>
          val annotation      = f.getAnnotation(classOf[ApiModelProperty])
          val position        = if (annotation.position() == Integer.MAX_VALUE) None else Some(annotation.position())
          val minimumValue    = if (annotation.minimumValue().isNaN) None else Option(annotation.minimumValue())
          val maximumValue    = if (annotation.maximumValue().isNaN) None else Option(annotation.maximumValue())
          val asModelProperty = toModelProperty(
            descriptor,
            position,
            annotation.required(),
            annotation.description().blankOption,
            annotation.allowableValues(),
            annotation.example().blankOption,
            minimumValue,
            maximumValue,
            annotation.defaultValue().blankOption,
            annotation.hidden()
          )

          descriptor.properties.find(_.mangledName == f.getName) map asModelProperty

        case f: Field =>
          val asModelProperty = toModelProperty(descriptor)
          descriptor.properties.find(_.mangledName == f.getName) map asModelProperty

      }

      val result = apiModel map { am =>
        Model(
          id = name,
          name = name,
          qualifiedName = klass.fullName.blankOption,
          description = am.description().blankOption,
          properties = fields.flatten,
          baseModel = am.parent.getName.blankOption,
          discriminator = am.discriminator.blankOption
        )
      } orElse Some(Model(name, name, klass.fullName.blankOption, properties = fields.flatten))
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
      if (csvString.isEmpty) {
        AllowableValues.AnyValue
      } else {
        val params = csvString.split(",").toList
        paramType match {
          case null     => AllowableValues.AllowableValuesList(params)
          case "string" => AllowableValues.AllowableValuesList(params)
        }
      }
    }
  }

  private def buildAllowableRangeValues(
      ranges: Array[String],
      inputStr: String,
      inclusive: Boolean
  ): AllowableValues.AllowableRangeValues = {
    var min: java.lang.Float = 0f
    var max: java.lang.Float = 0f

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

    AllowableValues.AllowableRangeValues(
      if (inclusive) Range.inclusive(min.toInt, max.toInt) else Range(min.toInt, max.toInt)
    )
  }

}

/** An instance of this class is used to hold the API documentation.
  */
class Swagger(
    val swaggerVersion: String,
    val apiVersion: String,
    val apiInfo: ApiInfo,
    val host: String = "",
    val extraSwaggerDefinition: Option[JValue] = None
) extends SwaggerEngine {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  /** Registers the documentation for an API with the given path.
    */
  def register(
      listingPath: String,
      resourcePath: String,
      description: Option[String],
      s: SwaggerSupportSyntax & SwaggerSupportBase,
      consumes: List[String],
      produces: List[String],
      protocols: List[String],
      authorizations: List[String]
  ) = {
    logger.debug(
      s"registering swagger api with: { listingPath: $listingPath, resourcePath: $resourcePath, description: $resourcePath, servlet: ${s.getClass} }"
    )
    val endpoints: List[Endpoint] = s.endpoints(resourcePath) collect { case m: Endpoint => m }
    _docs += listingPath -> Api(
      apiVersion,
      swaggerVersion,
      resourcePath,
      description,
      (produces ::: endpoints.flatMap(_.operations.flatMap(_.produces))).distinct,
      (consumes ::: endpoints.flatMap(_.operations.flatMap(_.consumes))).distinct,
      (protocols ::: endpoints.flatMap(_.operations.flatMap(_.schemes))).distinct,
      endpoints,
      s.models.toMap,
      (authorizations ::: endpoints.flatMap(_.operations.flatMap(_.authorizations))).distinct,
      0
    )
  }
}

case class Api(
    apiVersion: String,
    swaggerVersion: String,
    resourcePath: String,
    description: Option[String] = None,
    produces: List[String] = Nil,
    consumes: List[String] = Nil,
    protocols: List[String] = Nil,
    apis: List[Endpoint] = Nil,
    models: Map[String, Model] = Map.empty,
    authorizations: List[String] = Nil,
    position: Int = 0
) {
  def model(name: String) = models.get(name)
}

object ParamType extends Enumeration {
  type ParamType = Value

  /** A parameter carried in a POST body. * */
  val Body = Value("body")

  /** A parameter carried on the query string.
    *
    * E.g. http://example.com/foo?param=2
    */
  val Query = Value("query")

  /** A path parameter mapped to a Scalatra route.
    *
    * E.g. http://example.com/foo/2 where there's a route like get("/foo/:id").
    */
  val Path = Value("path")

  /** A parameter carried in an HTTP header. * */
  val Header = Value("header")

  val File = Value("file")

  val Form = Value("form")
}

sealed trait DataType {
  def name: String
}

object DataType {

  case class ValueDataType(name: String, format: Option[String] = None, qualifiedName: Option[String] = None)
      extends DataType
  case class ContainerDataType(name: String, typeArg: Option[DataType] = None, uniqueItems: Boolean = false)
      extends DataType

  val Void     = DataType("void")
  val String   = DataType("string")
  val Byte     = DataType("string", Some("byte"))
  val Int      = DataType("integer", Some("int32"))
  val Long     = DataType("integer", Some("int64"))
  val Float    = DataType("number", Some("float"))
  val Double   = DataType("number", Some("double"))
  val Boolean  = DataType("boolean")
  val Date     = DataType("string", Some("date"))
  val DateTime = DataType("string", Some("date-time"))

  object GenList {
    def apply(): DataType            = ContainerDataType("List")
    def apply(v: DataType): DataType = new ContainerDataType("List", Some(v))
  }

  object GenSet {
    def apply(): DataType            = ContainerDataType("Set", uniqueItems = true)
    def apply(v: DataType): DataType = new ContainerDataType("Set", Some(v), uniqueItems = true)
  }

  object GenArray {
    def apply(): DataType            = ContainerDataType("Array")
    def apply(v: DataType): DataType = new ContainerDataType("Array", Some(v))
  }

  object GenMap {
    def apply(): DataType            = ContainerDataType("Map")
    def apply(v: DataType): DataType = new ContainerDataType("Map", Some(v))
  }

  def apply(name: String, format: Option[String] = None, qualifiedName: Option[String] = None) =
    new ValueDataType(name, format, qualifiedName)
  def apply[T](implicit mf: Manifest[T]): DataType = fromManifest[T](using mf)

  private[this] val StringTypes               = Set[Class[?]](classOf[String], classOf[java.lang.String])
  private[this] def isString(klass: Class[?]) = StringTypes contains klass
  private[this] val BoolTypes                 = Set[Class[?]](classOf[Boolean], classOf[java.lang.Boolean])
  private[this] def isBool(klass: Class[?])   = BoolTypes contains klass

  private[swagger] def fromManifest[T](implicit mf: Manifest[T]): DataType = {
    fromScalaType(Reflector.scalaTypeOf[T])
  }
  private[swagger] def fromClass(klass: Class[?]): DataType   = fromScalaType(Reflector.scalaTypeOf(klass))
  private[swagger] def fromScalaType(st: ScalaType): DataType = {
    val klass = if (st.isOption && st.typeArgs.nonEmpty) st.typeArgs.head.erasure else st.erasure
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
    else if (
      classOf[scala.collection.Set[?]].isAssignableFrom(klass) || classOf[java.util.Set[?]].isAssignableFrom(klass)
    ) {
      if (st.typeArgs.nonEmpty) GenSet(fromScalaType(st.typeArgs.head))
      else GenSet()
    } else if (
      classOf[collection.Seq[?]].isAssignableFrom(klass) || classOf[java.util.List[?]].isAssignableFrom(klass)
    ) {
      if (st.typeArgs.nonEmpty) GenList(fromScalaType(st.typeArgs.head))
      else GenList()
    } else if (st.isMap) {
      if (st.typeArgs.nonEmpty) GenMap(fromScalaType(st.typeArgs.last))
      else GenMap()
    } else if (st.isArray || isCollection(klass)) {
      if (st.typeArgs.nonEmpty) GenArray(fromScalaType(st.typeArgs.head))
      else GenArray()
    } else {
      val stt = if (st.isOption) st.typeArgs.head else st
      new ValueDataType(stt.simpleName, qualifiedName = Option(stt.fullName))
    }
  }

  private[this] val IntTypes =
    Set[Class[?]](
      classOf[Int],
      classOf[java.lang.Integer],
      classOf[Short],
      classOf[java.lang.Short],
      classOf[BigInt],
      classOf[java.math.BigInteger]
    )
  private[this] def isInt(klass: Class[?]) = IntTypes.contains(klass)

  private[this] val DecimalTypes =
    Set[Class[?]](classOf[Double], classOf[java.lang.Double], classOf[BigDecimal], classOf[java.math.BigDecimal])
  private[this] def isDecimal(klass: Class[?]) = DecimalTypes contains klass

  private[this] val DateTypes =
    Set[Class[?]](classOf[java.time.LocalDate])
  private[this] def isDate(klass: Class[?]) = DateTypes.exists(_.isAssignableFrom(klass))
  private[this] val DateTimeTypes           =
    Set[Class[?]](
      classOf[JDate],
      classOf[java.time.LocalDateTime],
      classOf[java.time.ZonedDateTime],
      classOf[java.time.OffsetDateTime],
      classOf[java.time.Instant]
    )
  private[this] def isDateTime(klass: Class[?]) = DateTimeTypes.exists(_.isAssignableFrom(klass))

  private[this] def isCollection(klass: Class[?]) =
    classOf[collection.Iterable[?]].isAssignableFrom(klass) ||
      classOf[java.util.Collection[?]].isAssignableFrom(klass)

}

case class ApiInfo(
    title: String,
    description: String,
    termsOfServiceUrl: String,
    contact: ContactInfo,
    license: LicenseInfo
)

case class ContactInfo(name: String, url: String, email: String)

case class LicenseInfo(name: String, url: String)

trait AllowableValues

object AllowableValues {
  case object AnyValue                               extends AllowableValues
  case class AllowableValuesList[T](values: List[T]) extends AllowableValues
  case class AllowableRangeValues(values: Range)     extends AllowableValues

  def apply(): AllowableValues                   = empty
  def apply[T](values: T*): AllowableValues      = apply(values.toList)
  def apply[T](values: List[T]): AllowableValues = AllowableValuesList(values)
  def apply(values: Range): AllowableValues      = AllowableRangeValues(values)
  def empty                                      = AnyValue
}

case class Parameter(
    name: String,
    `type`: DataType,
    description: Option[String] = None,
    paramType: ParamType.ParamType = ParamType.Query,
    defaultValue: Option[String] = None,
    allowableValues: AllowableValues = AllowableValues.AnyValue,
    required: Boolean = true,
    // TODO Add collectionFormat: Option[String] for Swagger 2.0
    position: Int = 0,
    example: Option[String] = None,
    minimumValue: Option[Double] = None,
    maximumValue: Option[Double] = None,
    hidden: Boolean = false
)

case class ModelProperty(
    `type`: DataType,
    position: Int = 0,
    required: Boolean = false,
    description: Option[String] = None,
    allowableValues: AllowableValues = AllowableValues.AnyValue,
    example: Option[String] = None,
    default: Option[String] = None,
    minimumValue: Option[Double] = None,
    maximumValue: Option[Double] = None,
    hidden: Boolean = false
)

case class Model(
    id: String,
    name: String,
    qualifiedName: Option[String] = None,
    description: Option[String] = None,
    properties: List[(String, ModelProperty)] = Nil,
    baseModel: Option[String] = None,
    discriminator: Option[String] = None
) {

  def setRequired(property: String, required: Boolean): Model = {
    val prop = properties.find(_._1 == property).get
    copy(properties = (property -> prop._2.copy(required = required)) :: properties)
  }

  def getVisibleProperties: Seq[(String, ModelProperty)] = properties.filter(!_._2.hidden)
}

case class LoginEndpoint(url: String)
case class TokenRequestEndpoint(url: String, clientIdName: String, clientSecretName: String)
case class TokenEndpoint(url: String, tokenName: String)

trait AuthorizationType {
  def `type`: String
  def keyName: String
  def description: String
}
case class OAuth(
    scopes: List[String],
    grantTypes: List[GrantType],
    keyName: String = "oauth2",
    description: String = ""
) extends AuthorizationType {
  override val `type` = "oauth2"
}
case class ApiKey(keyName: String, passAs: String = "header", description: String = "") extends AuthorizationType {
  override val `type` = "apiKey"
}

case class BasicAuth(keyName: String, description: String = "") extends AuthorizationType {
  override val `type` = "basic"
}

trait GrantType {
  def `type`: String
}
case class ImplicitGrant(loginEndpoint: LoginEndpoint, tokenName: String) extends GrantType {
  def `type` = "implicit"
}
case class AuthorizationCodeGrant(tokenRequestEndpoint: TokenRequestEndpoint, tokenEndpoint: TokenEndpoint)
    extends GrantType {
  def `type` = "authorization_code"
}
case class ApplicationGrant(tokenEndpoint: TokenEndpoint) extends GrantType {
  def `type` = "application"
}

case class Operation(
    method: HttpMethod,
    operationId: String,
    responseClass: DataType,
    summary: String,
    position: Int,
    description: Option[String] = None,
    deprecated: Boolean = false,
    parameters: List[Parameter] = Nil,
    responseMessages: List[ResponseMessage] = Nil,
    consumes: List[String] = Nil,
    produces: List[String] = Nil,
    schemes: List[String] = Nil,
    authorizations: List[String] = Nil,
    tags: List[String] = Nil
) {

  def getVisibleParameters: List[Parameter] = parameters.filter(!_.hidden)
}

case class Endpoint(path: String, description: Option[String] = None, operations: List[Operation] = Nil)

case class ResponseMessage(code: Int, message: String, responseModel: Option[String] = None)
