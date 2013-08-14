package org.scalatra.swagger

import org.json4s._
import DefaultReaders._
import DefaultWriters._
import org.scalatra.util.RicherString._
import JsonDSL._
import org.scalatra.HttpMethod

trait SwaggerJsonFormats {
  implicit def HttpMethodJsonFormats: JsonFormat[HttpMethod]
  implicit def DataTypeJsonFormat: JsonFormat[DataType]
  implicit def AllowableValuesJsonFormats: JsonFormat[AllowableValues]
  implicit def ModelRefFormats: JsonFormat[ModelRef]
  implicit def ModelPropertyJsonFormats: JsonFormat[ModelProperty]
  implicit def ModelJsonFormats: JsonFormat[Model]
  implicit def ParameterJsonFormats: JsonFormat[Parameter]
  implicit def ResponseMessageJsonFormat: JsonFormat[ResponseMessage[_]]
  implicit def OperationJsonFormat: JsonFormat[Operation]
  implicit def EndpointJsonFormat: JsonFormat[Endpoint]
  implicit def ApiJsonFormats: JsonFormat[Api]
  implicit def ApiInfoJsonFormats: JsonFormat[ApiInfo]
  implicit def GrantTypeJsonFormats: JsonFormat[GrantType]
  implicit def AuthorizationTypeJsonFormats: JsonFormat[AuthorizationType]
  implicit def ApiListingReferenceJsonFormat: JsonFormat[ApiListingReference]
  implicit def ResourceListingJsonFormat: JsonFormat[ResourceListing]
}
trait DefaultSwaggerJsonFormats extends SwaggerJsonFormats {

  import AllowableValues._
  implicit def traversableWriter[V](implicit valueWriter: Writer[V]): Writer[List[V]] =
    new Writer[List[V]] {
      def write(obj: List[V]): JValue = JArray(obj.map(valueWriter.write))
    }

  implicit val HttpMethodJsonFormats: JsonFormat[HttpMethod] = new JsonFormat[HttpMethod] {
    def write(obj: HttpMethod): JValue = JString(obj.toString)

    def read(value: JValue): HttpMethod = value match {
      case JString(v) => HttpMethod(v)
      case _ => throw new MappingException("Couldn't get a http method from: " + value)
    }
  }

  implicit val DataTypeJsonFormat: JsonFormat[DataType] = new JsonFormat[DataType] {
    private[this] val simpleTypes =
      Set("int32", "int64", "float", "double", "string", "byte", "boolean", "date", "date-time", "array")
    private[this] def isSimpleType(name: String) = simpleTypes contains name

    def write(obj: DataType): JValue = obj match {
      case DataType.ValueDataType(name, Some(format), _) =>
        ("type" -> name) ~ ("format" -> format)
      case DataType.ValueDataType("string", format, _) =>
        ("type" -> "string") ~ ("format" -> format)
      case DataType.ValueDataType("boolean", format, _) =>
        ("type" -> "boolean") ~ ("format" -> format)
      case DataType.ValueDataType("void", format, _) =>
        ("type" -> "void") ~ ("format" -> format)
      case DataType.ContainerDataType("List" | "Array", Some(dt), _) =>
        ("type" -> "array") ~ ("items" -> write(dt))
      case DataType.ContainerDataType("List" | "Array", _, _) =>
        ("type" -> "array") ~ ("format" -> None)
      case DataType.ContainerDataType("Set", Some(dt), _) =>
        ("type" -> "array") ~ ("items" -> write(dt)) ~ ("uniqueItems" -> true)
      case DataType.ContainerDataType("Set", _, _) =>
        ("type" -> "array") ~ ("uniqueItems" -> true)
      case DataType.ValueDataType(name, _, qualifiedName) =>
        ("type" -> name) ~ ("qualifiedType" -> qualifiedName)
    }

    private[this] def str(jv: JValue) = jv.getAs[String].flatMap(_.blankOption)
    def read(value: JValue): DataType = {
      def karmaIsABitch = throw new MappingException("Couldn't determine the type for this data type from " + value)
      val t = str(value \ "format") orElse str(value \ "type") orElse str(value \ "$ref") getOrElse karmaIsABitch
      if (isSimpleType(t)) {
        if (t == "array") {
          val items = value \ "items" match {
            case JNothing => None
            case jv => Some(read(jv))
          }
          value \ "uniqueItems" match {
            case JBool(true) =>
              items map (DataType.GenSet(_)) getOrElse DataType.GenSet()
            case _ =>
              items map (DataType.GenList(_)) getOrElse DataType.GenList()
          }
        } else {
          DataType((value \ "type").as[String], format = str(value \ "format"))
        }
      } else {
        DataType(t, qualifiedName = str(value \ "qualifiedType"))
      }
    }
  }


  implicit val AllowableValuesJsonFormats: JsonFormat[AllowableValues] = new JsonFormat[AllowableValues] {
    def write(obj: AllowableValues): JValue = obj match {
      case AnyValue => JNothing
      case v: AllowableValuesList[_] => "enum" -> v.toJValue
      case AllowableRangeValues(range) => ("minimum" -> range.start) ~ ("maximum" -> range.end)

    }

    def read(value: JValue): AllowableValues = value match {
      case JObject(flds) if flds.exists(_._1 == "enum") =>
        value \ "enum" match {
          case JArray(entries) => entries.headOption match {
            case Some(_: JInt) => AllowableValuesList(entries.map(_.as[Int]))(DefaultJsonFormats.GenericFormat[Int])
            case Some(_: JDouble) => AllowableValuesList(entries.map(_.as[Double]))(DefaultJsonFormats.GenericFormat[Double])
            case Some(_: JDecimal) =>  AllowableValuesList(entries.map(_.as[BigDecimal]))(DefaultJsonFormats.GenericFormat[BigDecimal])
            case Some(_: JBool) => AllowableValuesList(entries.map(_.as[Boolean]))(DefaultJsonFormats.GenericFormat[Boolean])
            case Some(_: JString) => AllowableValuesList(entries.map(_.as[String]))(DefaultJsonFormats.GenericFormat[String])
            case _ => AnyValue
          }
          case _ => AnyValue
        }
      case JObject(flds) if flds.exists(_._1 == "minimum") && flds.exists(_._1 == "maximum") =>
        AllowableRangeValues((value \ "minimum").as[Int] to (value \ "maximum").as[Int])
      case _ => AnyValue
    }
  }

  implicit val ModelRefFormats: JsonFormat[ModelRef] = new JsonFormat[ModelRef] {
    def write(obj: ModelRef): JValue =
      ("type" -> obj.`type`.blankOption) ~ ("$ref" -> obj.ref.flatMap(_.blankOption))

    def read(value: JValue): ModelRef =
      ModelRef(
        (value \ "type").getAs[String].flatMap(_.blankOption).orNull,
        (value \ "$ref").getAs[String].flatMap(_.blankOption)
      )
  }

  implicit val ModelPropertyJsonFormats: JsonFormat[ModelProperty] = new JsonFormat[ModelProperty] {
    def write(x: ModelProperty): JValue = {
      val json =
        ("description" -> x.description) ~
        ("position" -> x.position) ~
        ("items" -> Formats.write(x.items)) ~
        ("required" -> x.required)
      json merge Formats.write(x.`type`) merge Formats.write(x.allowableValues)
    }

    def read(json: JValue): ModelProperty = {

      ModelProperty(
        `type` = json.as[DataType],
        position = (json \ "position").getAsOrElse(0),
        json \ "required" match {
          case JString(s) => s.toCheckboxBool
          case JBool(value) => value
          case _ => false
        },
        description = (json \ "description").getAs[String].flatMap(_.blankOption),
        allowableValues = json.as[AllowableValues],
        items = (json \ "items").getAs[ModelRef].filter(e =>  e.`type`.nonBlank || e.ref.nonEmpty)
      )

    }
  }

  implicit val ModelJsonFormats: JsonFormat[Model] = new JsonFormat[Model] {
    def write(x: Model): JValue = {
      val required = for ((key, value) <- x.properties if value.required) yield key
      ("id" -> x.id) ~
      ("name" -> x.name) ~
      ("qualifiedType" -> x.qualifiedName) ~
      ("description" -> x.description) ~
      ("required" -> required) ~
      ("extends" -> x.baseModel.filter(s => s.nonBlank && !s.trim.equalsIgnoreCase("void"))) ~
      ("discriminator" -> x.discriminator) ~
      ("properties" -> (x.properties map { case (k, v) => k -> Formats.write(v)}))
    }

    def read(json: JValue): Model = {
      val properties = json \ "properties" match {
        case JObject(entries) => {
          for((key, value) <- entries) yield key -> value.as[ModelProperty]
        }
        case _ => Nil
      }

      Model(
        (json \ "id").getAsOrElse(""),
        (json \ "name").getAsOrElse((json \ "id").as[String]),
        (json \ "qualifiedType").getAs[String].flatMap(_.blankOption),
        (json \ "description").getAs[String].flatMap(_.blankOption),
        properties,
        (json \ "extends").getAs[String].flatMap(_.blankOption),
        (json \ "discriminator").getAs[String].flatMap(_.blankOption)
      )

    }
  }

  implicit val ResponseMessageJsonFormat: JsonFormat[ResponseMessage[_]] = new JsonFormat[ResponseMessage[_]] {
    def write(obj: ResponseMessage[_]): JValue = obj match {
      case StringResponseMessage(code, message) =>
        ("code" -> code) ~ ("message" -> message)
      case _ => throw new MappingException("Couldn't map " + obj + " to json")
    }

    def read(value: JValue): ResponseMessage[_] =
      StringResponseMessage((value \ "code").as[Int], (value \ "message").as[String])
  }

  implicit val ParameterJsonFormats: JsonFormat[Parameter] = new JsonFormat[Parameter] {
    def write(x: Parameter): JValue = {
      val output =
        ("name" -> x.name) ~
        ("description" -> x.description) ~
        ("defaultValue" -> x.defaultValue) ~
        ("required" -> x.required) ~
        ("allowMultiple" -> x.allowMultiple) ~
        ("paramType" -> x.paramType.toString) ~
        ("paramAccess" -> x.paramAccess)

      output merge Formats.write(x.`type`) merge Formats.write(x.allowableValues)
    }

    def read(json: JValue): Parameter = {
      val t = json.as[DataType]
      Parameter(
        (json \ "name").getAsOrElse(""),
        t,
        (json \ "description").getAs[String].flatMap(_.blankOption),
        (json \ "notes").getAs[String].flatMap(_.blankOption),
        (json \ "paramType").getAs[String].flatMap(_.blankOption).map(ParamType.withName).getOrElse(ParamType.Query),
        json \ "defaultValue" match {
          case JInt(num) => Some(num.toString)
          case JBool(value) => Some(value.toString)
          case JString(s) => Some(s)
          case JDouble(num) => Some(num.toString)
          case JDecimal(num) => Some(num.toString)
          case _ => None
        },
        (json \ "allowableValues").as[AllowableValues],
        json \ "required" match {
          case JString(s) => s.toBoolean
          case JBool(value) => value
          case _ => false
        },
        (json \ "allowMultiple").getAsOrElse(false),
        (json \ "paramAccess").getAs[String].flatMap(_.blankOption)
     )
    }
  }

  implicit val OperationJsonFormat: JsonFormat[Operation] = new JsonFormat[Operation] {
    def write(obj: Operation): JValue = {
      val json = ("method" -> Formats.write(obj.method)) ~
                ("summary" -> obj.summary) ~
                ("position" -> obj.position) ~
                ("notes" -> obj.notes) ~
                ("deprecated" -> obj.deprecated) ~
                ("nickname" -> obj.nickname) ~
                ("parameters" -> Formats.write(obj.parameters)) ~
                ("responseMessages" -> Formats.write(obj.responseMessages)) ~
                ("supportedContentTypes" -> obj.supportedContentTypes) ~
                ("consumes" -> obj.consumes) ~
                ("produces" -> obj.produces) ~
                ("protocols" -> obj.protocols) ~
                ("authorizations" -> obj.authorizations)
      json merge Formats.write(obj.responseClass)
    }

    def read(value: JValue): Operation =
      Operation(
        (value \ "method").as[HttpMethod],
        value.as[DataType],
        (value \ "summary").as[String],
        (value \ "position").as[Int],
        (value \ "notes").getAs[String].flatMap(_.blankOption),
        (value \ "deprecated").getAs[Boolean] getOrElse false,
        (value \ "nickname").getAs[String].flatMap(_.blankOption),
        (value \ "parameters").as[List[Parameter]],
        (value \ "responseMessages").as[List[ResponseMessage[_]]],
        (value \ "supportedContentTypes").as[List[String]],
        (value \ "consumes").as[List[String]],
        (value \ "produces").as[List[String]],
        (value \ "protocols").as[List[String]],
        (value \ "authorizations").as[List[String]]
      )
  }

  implicit val EndpointJsonFormat: JsonFormat[Endpoint] = new JsonFormat[Endpoint] {
    def write(obj: Endpoint): JValue =
      ("path" -> obj.path) ~
      ("description" -> obj.description) ~
      ("operations" -> Formats.write(obj.operations))

    def read(value: JValue): Endpoint =
      Endpoint(
        (value \ "path").as[String],
        (value \ "description").getAs[String].flatMap(_.blankOption),
        (value \ "operations").as[List[Operation]])
  }

  implicit val ApiJsonFormats: JsonFormat[Api] = new JsonFormat[Api] {
    def write(x: Api): JValue = {
      ("apiVersion" -> x.apiVersion) ~
      ("swaggerVersion" -> x.swaggerVersion) ~
      ("basePath" -> x.basePath) ~
      ("resourcePath" -> x.resourcePath) ~
      ("produces" -> (x.produces match {
        case Nil => JNothing
        case e => Formats.write(e)
      })) ~
      ("consumes" -> (x.consumes match {
        case Nil => JNothing
        case e => Formats.write(e)
      })) ~
      ("protocols" -> (x.protocols match {
        case Nil => JNothing
        case e => Formats.write(e)
      })) ~
      ("authorizations" -> (x.authorizations match {
        case Nil => JNothing
        case e => Formats.write(e)
      })) ~
      ("apis" -> (x.apis match {
        case Nil => JNothing
        case e => Formats.write(e)
      })) ~
      ("models" -> (x.models match {
        case x if x.isEmpty => JNothing
        case e => Formats.write(e)
      }))
    }

    def read(json: JValue): Api = {
      Api(
        (json \ "apiVersion").getAsOrElse(""),
        (json \ "swaggerVersion").getAsOrElse(""),
        (json \ "basePath").getAsOrElse(""),
        (json \ "resourcePath").getAsOrElse(""),
        (json \ "description").getAs[String].flatMap(_.blankOption),
        (json \ "produces").getAsOrElse(List.empty[String]),
        (json \ "consumes").getAsOrElse(List.empty[String]),
        (json \ "protocols").getAsOrElse(List.empty[String]),
        (json \ "apis").getAsOrElse(List.empty[Endpoint]),
        (json \ "models").getAs[Map[String, Model]].getOrElse(Map.empty),
        (json \ "authorizations").getAsOrElse(List.empty[String]),
        (json \ "position").getAsOrElse(0)
      )
    }
  }

  implicit val ApiInfoJsonFormats: JsonFormat[ApiInfo] = new JsonFormat[ApiInfo] {
    def write(obj: ApiInfo): JValue =
      ("title" -> obj.title) ~
      ("description" -> obj.description) ~
      ("termsOfServiceUrl" -> obj.termsOfServiceUrl) ~
      ("contact" -> obj.contact) ~
      ("license" -> obj.license) ~
      ("licenseUrl" -> obj.licenseUrl)

    def read(value: JValue): ApiInfo =
      ApiInfo(
        (value \ "title").as[String],
        (value \ "description").as[String],
        (value \ "termsOfServiceUrl").as[String],
        (value \ "contact").as[String],
        (value \ "license").as[String],
        (value \ "licenseUrl").as[String]
      )
  }

  implicit val ApiListingReferenceJsonFormat: JsonFormat[ApiListingReference] = new JsonFormat[ApiListingReference] {
    def write(obj: ApiListingReference): JValue =
      ("path" -> obj.path) ~
      ("description" -> obj.description) ~
      ("position" -> obj.position)

    def read(value: JValue): ApiListingReference =
      ApiListingReference(
        (value \ "path").as[String],
        (value \ "description").getAs[String].flatMap(_.blankOption),
        (value \ "position").getAsOrElse(0)
      )
  }


  implicit val GrantTypeJsonFormats: JsonFormat[GrantType] = new JsonFormat[GrantType] {
    def write(obj: GrantType): JValue = obj match {
      case ImplicitGrant(login, tokenName) =>
        ("type" -> "implicit") ~
        ("loginEndpoint" -> (("url" -> login.url): JValue)) ~
        ("tokenName" -> tokenName)
      case AuthorizationCodeGrant(tokenRequest, tokenEndpoint) =>
        ("type" -> "authorization_code") ~
        ("tokenRequestEndpoint" ->
          (("url" -> tokenRequest.url) ~
          ("clientIdName" -> tokenRequest.clientIdName) ~
          ("clientSecretName" -> tokenRequest.clientSecretName))) ~
        ("tokenEndpoint" -> (
          ("url" -> tokenEndpoint.url) ~
          ("tokenName" -> tokenEndpoint.tokenName)))
    }

    def read(value: JValue): GrantType = value \ "type" match {
      case JString("implicit") =>
        ImplicitGrant(
          LoginEndpoint((value \ "loginEndpoint" \ "url" ).as[String]),
          (value \ "tokenName").as[String])
      case JString("authorization_code") =>
        AuthorizationCodeGrant(
          TokenRequestEndpoint(
            (value \ "tokenRequestEndpoint" \ "url").as[String],
            (value \ "tokenRequestEndpoint" \ "clientIdName").as[String],
            (value \ "tokenRequestEndpoint" \ "clientSecretName").as[String]
          ),
          TokenEndpoint(
            (value \ "tokenEndpoint" \ "url").as[String],
            (value \ "tokenEndpoint" \ "tokenName").as[String]))
      case _ => throw new MappingException("Couldn't map " + value + " to a grant type")
    }
  }

  implicit val AuthorizationTypeJsonFormats: JsonFormat[AuthorizationType] = new JsonFormat[AuthorizationType] {
    def write(obj: AuthorizationType): JValue = obj match {
      case OAuth(scopes, grantTypes) =>
        ("type" -> obj.`type`) ~
        ("scopes" -> scopes) ~
        ("grantTypes" ->
          (for(t <- grantTypes) yield {
            (t.`type`, Formats.write(t))
          }).toMap)
      case ApiKey(keyname, passAs) =>
        ("type" -> obj.`type`) ~
        ("passAs" -> passAs) ~
        ("keyname" -> keyname)
    }

    def read(value: JValue): AuthorizationType = value \ "type" match {
      case JString("apiKey") =>
        ApiKey((value \ "keyname").as[String], (value \ "passAs").as[String])
      case JString("oauth2") =>
        OAuth((value \ "scopes").as[List[String]], (value \ "grantTypes").as[List[GrantType]])
      case _ => throw new MappingException("Couldn't map " + value + " to an authorization type")
    }
  }

  implicit val ResourceListingJsonFormat: JsonFormat[ResourceListing] = new JsonFormat[ResourceListing] {
    def write(obj: ResourceListing): JValue =
      ("apiVersion" -> obj.apiVersion) ~
      ("swaggerVersion" -> obj.swaggerVersion) ~
      ("apis" -> Formats.write(obj.apis)) ~
      ("authorizations" -> Formats.write(obj.authorizations)) ~
      ("info" -> Formats.write(obj.info))

    def read(value: JValue): ResourceListing =
      ResourceListing(
        (value \ "apiVersion").as[String],
        (value \ "swaggerVersion").as[String],
        (value \ "apis").as[List[ApiListingReference]],
        (value \ "authorizations").as[List[AuthorizationType]],
        (value \ "info").getAs[ApiInfo]
      )
  }

}

object DefaultSwaggerJsonFormats extends DefaultSwaggerJsonFormats