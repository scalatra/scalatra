package org.scalatra.swagger

import org.json4s._
import DefaultReaders._
import org.scalatra.util.RicherString._
import JsonDSL._
import org.scalatra.HttpMethod
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import java.util.{ Date => JDate }
import org.json4s.ext.{ EnumNameSerializer, JodaTimeSerializers }
import java.lang.reflect.Type
import java.text.SimpleDateFormat

object SwaggerSerializers {
  import AllowableValues._
  private val simpleTypes =
    Set("int32", "int64", "float", "double", "string", "byte", "boolean", "date", "date-time", "array")
  private def isSimpleType(name: String) = simpleTypes contains name

  private def str(jv: JValue): Option[String] = jv.getAs[String].flatMap(_.blankOption)

  private[swagger] def dontAddOnEmpty(key: String, value: List[String])(json: JValue) = {
    if (value.nonEmpty) json merge JObject(List(key -> JArray(value map (JString(_))))) else json
  }

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  object SwaggerFormats {
    val serializers = JodaTimeSerializers.all ++ Seq(
      new EnumNameSerializer(ParamType),
      new HttpMethodSerializer,
      new AllowableValuesSerializer,
      new ModelPropertySerializer,
      new ModelSerializer,
      new ResponseMessageSerializer,
      new ParameterSerializer,
      new GrantTypeSerializer)
  }
  trait SwaggerFormats extends DefaultFormats {
    private[this] val self = this
    def withAuthorizationTypeSerializer(serializer: Serializer[AuthorizationType]): SwaggerFormats = new SwaggerFormats {
      override val customSerializers: List[Serializer[_]] = serializer :: SwaggerFormats.serializers
    }

    override def +[A](newSerializer: FieldSerializer[A]): SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = self.typeHints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] =
        (newSerializer.mf.runtimeClass -> newSerializer) :: self.fieldSerializers
      override val wantsBigDecimal: Boolean = self.wantsBigDecimal
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = self.companions
      override val strict: Boolean = self.strict
    }

    override def +(extraHints: TypeHints): SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = self.typeHints + extraHints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = self.fieldSerializers
      override val wantsBigDecimal: Boolean = self.wantsBigDecimal
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = self.companions
      override val strict: Boolean = self.strict
    }

    override def withCompanions(comps: (Class[_], AnyRef)*): SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = self.typeHints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = self.fieldSerializers
      override val wantsBigDecimal: Boolean = self.wantsBigDecimal
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = comps.toList ::: self.companions
      override val strict: Boolean = self.strict
    }

    override def withDouble: SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = self.typeHints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = self.fieldSerializers
      override val wantsBigDecimal: Boolean = false
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = self.companions
      override val strict: Boolean = self.strict
    }

    override def withBigDecimal: SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = self.typeHints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = self.fieldSerializers
      override val wantsBigDecimal: Boolean = true
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = self.companions
      override val strict: Boolean = self.strict
    }

    override def withHints(hints: TypeHints): SwaggerFormats = new SwaggerFormats {
      override val dateFormat: DateFormat = self.dateFormat
      override val typeHintFieldName: String = self.typeHintFieldName
      override val parameterNameReader: org.json4s.reflect.ParameterNameReader = self.parameterNameReader
      override val typeHints: TypeHints = hints
      override val customSerializers: List[Serializer[_]] = self.customSerializers
      override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = self.fieldSerializers
      override val wantsBigDecimal: Boolean = self.wantsBigDecimal
      override val primitives: Set[Type] = self.primitives
      override val companions: List[(Class[_], AnyRef)] = self.companions
      override val strict: Boolean = self.strict
    }

    override def lossless: SwaggerFormats = new SwaggerFormats {
      override protected def dateFormatter: SimpleDateFormat = DefaultFormats.losslessDate()
    }

    override val customSerializers: List[Serializer[_]] = new AuthorizationTypeSerializer :: SwaggerFormats.serializers
    override def ++(newSerializers: Traversable[Serializer[_]]): SwaggerFormats = newSerializers.foldLeft(this)(_ + _)
    override def +(newSerializer: Serializer[_]): SwaggerFormats = new SwaggerFormats {
      override val customSerializers: List[Serializer[_]] = newSerializer :: SwaggerFormats.this.customSerializers
    }

    override val dateFormat = new DateFormat {
      def format(d: JDate) = new DateTime(d).toString(Iso8601Date)
      def parse(s: String) = try {
        Option(Iso8601Date.parseDateTime(s).toDate)
      } catch {
        case _: Throwable ⇒ None
      }
    }
  }

  private[swagger] val formats: SwaggerFormats = new SwaggerFormats {}

  val defaultFormats: SwaggerFormats =
    formats ++ Seq(new OperationSerializer, new EndpointSerializer, new ApiSerializer)

  class HttpMethodSerializer extends CustomSerializer[HttpMethod](implicit formats => ({
    case JString(method) => HttpMethod(method)
  }, {
    case method: HttpMethod => JString(method.toString)
  }))

  def writeDataType(dataType: DataType, key: String = "type")(implicit formats: Formats): JValue = dataType match {
    case DataType.ValueDataType(name, Some(format), _) =>
      ("type" -> name) ~ ("format" -> format)
    case DataType.ValueDataType("string", format, _) =>
      ("type" -> "string") ~ ("format" -> format)
    case DataType.ValueDataType("boolean", format, _) =>
      ("type" -> "boolean") ~ ("format" -> format)
    case DataType.ValueDataType("void", format, _) =>
      ("type" -> "void") ~ ("format" -> format)
    case DataType.ContainerDataType("List" | "Array", Some(dt), _) =>
      ("type" -> "array") ~ ("items" -> writeDataType(dt, "$ref"))
    case DataType.ContainerDataType("List" | "Array", _, _) =>
      ("type" -> "array") ~ ("format" -> None)
    case DataType.ContainerDataType("Set", Some(dt), _) =>
      ("type" -> "array") ~ ("items" -> writeDataType(dt, "$ref")) ~ ("uniqueItems" -> true)
    case DataType.ContainerDataType("Set", _, _) =>
      ("type" -> "array") ~ ("uniqueItems" -> true)
    case DataType.ValueDataType(name, _, qualifiedName) =>
      (key -> name): JValue //~ ("qualifiedType" -> qualifiedName)
  }

  def readDataType(value: JValue)(implicit formats: Formats): DataType = {
    def karmaIsABitch = throw new MappingException("Couldn't determine the type for this data type from " + value)
    val t = str(value \ "format") orElse str(value \ "type") orElse str(value \ "$ref") getOrElse karmaIsABitch
    if (isSimpleType(t)) {
      if (t == "array") {
        val items = value \ "items" match {
          case JNothing => None
          case jv => Some(readDataType(jv))
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

  class AllowableValuesSerializer extends CustomSerializer[AllowableValues](implicit formats => ({
    case value @ JObject(flds) if flds.exists(_._1 == "enum") =>
      value \ "enum" match {
        case JArray(entries) => entries.headOption match {
          case Some(_: JInt) => AllowableValuesList(entries.map(_.as[Int]))
          case Some(_: JDouble) => AllowableValuesList(entries.map(_.as[Double]))
          case Some(_: JDecimal) => AllowableValuesList(entries.map(_.as[BigDecimal]))
          case Some(_: JBool) => AllowableValuesList(entries.map(_.as[Boolean]))
          case Some(_: JString) => AllowableValuesList(entries.map(_.as[String]))
          case _ => AnyValue
        }
        case _ => AnyValue
      }
    case value @ JObject(flds) if flds.exists(_._1 == "minimum") && flds.exists(_._1 == "maximum") =>
      AllowableRangeValues((value \ "minimum").as[Int] to (value \ "maximum").as[Int])
    case _ => AnyValue
  }, {
    case AnyValue => JNothing
    case AllowableValuesList(values) => ("enum" -> Extraction.decompose(values)): JValue
    case AllowableRangeValues(range) => ("minimum" -> range.start) ~ ("maximum" -> range.end)
  }))

  class ModelPropertySerializer extends CustomSerializer[ModelProperty](implicit formats => ({
    case json: JObject =>
      ModelProperty(
        `type` = readDataType(json),
        position = (json \ "position").getAsOrElse(0),
        json \ "required" match {
          case JString(s) => s.toCheckboxBool
          case JBool(value) => value
          case _ => false
        },
        description = (json \ "description").getAs[String].flatMap(_.blankOption),
        allowableValues = json.extract[AllowableValues],
        items = None)
  }, {
    case x: ModelProperty =>
      val json: JValue = ("description" -> x.description) ~ ("position" -> x.position)
      (json merge writeDataType(x.`type`, "$ref")) merge Extraction.decompose(x.allowableValues)
  }))

  class ModelSerializer extends CustomSerializer[Model](implicit formats => ({
    case json: JObject =>
      val properties = json \ "properties" match {
        case JObject(entries) => {
          for ((key, value) <- entries) yield key -> value.extract[ModelProperty]
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
  }, {
    case x: Model =>
      val required = for ((key, value) <- x.properties if value.required) yield key
      ("id" -> x.id) ~
        ("name" -> x.name) ~
        ("qualifiedType" -> x.qualifiedName) ~
        ("description" -> x.description) ~
        ("required" -> required) ~
        ("extends" -> x.baseModel.filter(s => s.nonBlank && !s.trim.equalsIgnoreCase("VOID"))) ~
        ("discriminator" -> x.discriminator) ~
        ("properties" -> (x.properties.sortBy { case (_, p) ⇒ p.position } map { case (k, v) => k -> Extraction.decompose(v) }))
  }))

  class ResponseMessageSerializer extends CustomSerializer[ResponseMessage[_]](implicit formats => ({
    case value: JObject =>
      StringResponseMessage((value \ "code").as[Int], (value \ "message").as[String])
  }, {
    case StringResponseMessage(code, message) =>
      ("code" -> code) ~ ("message" -> message)
  }))

  class ParameterSerializer extends CustomSerializer[Parameter](implicit formats => ({
    case json: JObject =>
      val t = readDataType(json)
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
        (json \ "allowableValues").extract[AllowableValues],
        json \ "required" match {
          case JString(s) => s.toBoolean
          case JBool(value) => value
          case _ => false
        },
        (json \ "paramAccess").getAs[String].flatMap(_.blankOption)
      )
  }, {
    case x: Parameter =>
      val output =
        ("name" -> x.name) ~
          ("description" -> x.description) ~
          ("defaultValue" -> x.defaultValue) ~
          ("required" -> x.required) ~
          ("paramType" -> x.paramType.toString) ~
          ("paramAccess" -> x.paramAccess)

      (output merge writeDataType(x.`type`)) merge Extraction.decompose(x.allowableValues)
  }))

  class OperationSerializer extends CustomSerializer[Operation](implicit formats => ({
    case value =>
      Operation(
        (value \ "method").extract[HttpMethod],
        readDataType(value),
        (value \ "summary").extract[String],
        (value \ "position").extract[Int],
        (value \ "notes").extractOpt[String].flatMap(_.blankOption),
        (value \ "deprecated").extractOpt[Boolean] getOrElse false,
        (value \ "nickname").extractOpt[String].flatMap(_.blankOption),
        (value \ "parameters").extract[List[Parameter]],
        (value \ "responseMessages").extract[List[ResponseMessage[_]]],
        (value \ "consumes").extract[List[String]],
        (value \ "produces").extract[List[String]],
        (value \ "protocols").extract[List[String]],
        (value \ "authorizations").extract[List[String]]
      )
  }, {
    case obj: Operation =>
      val json = ("method" -> Extraction.decompose(obj.method)) ~
        ("summary" -> obj.summary) ~
        ("position" -> obj.position) ~
        ("notes" -> obj.notes.flatMap(_.blankOption).getOrElse("")) ~
        ("deprecated" -> obj.deprecated) ~
        ("nickname" -> obj.nickname) ~
        ("parameters" -> Extraction.decompose(obj.parameters.sortBy(_.position))) ~
        ("responseMessages" -> (if (obj.responseMessages.nonEmpty) Some(Extraction.decompose(obj.responseMessages)) else None))

      val consumes = dontAddOnEmpty("consumes", obj.consumes)_
      val produces = dontAddOnEmpty("produces", obj.produces)_
      val protocols = dontAddOnEmpty("protocols", obj.protocols)_
      val authorizations = dontAddOnEmpty("authorizations", obj.authorizations)_
      val r = (consumes andThen produces andThen authorizations andThen protocols)(json)
      r merge writeDataType(obj.responseClass)
  }))

  class EndpointSerializer extends CustomSerializer[Endpoint](implicit formats => ({
    case value =>
      Endpoint(
        (value \ "path").extract[String],
        (value \ "description").extractOpt[String].flatMap(_.blankOption),
        (value \ "operations").extract[List[Operation]])
  }, {
    case obj: Endpoint =>
      ("path" -> obj.path) ~
        ("description" -> obj.description) ~
        ("operations" -> Extraction.decompose(obj.operations))
  }))

  class ApiSerializer extends CustomSerializer[Api](implicit formats => ({
    case json =>
      Api(
        (json \ "apiVersion").extractOrElse(""),
        (json \ "swaggerVersion").extractOrElse(""),
        (json \ "resourcePath").extractOrElse(""),
        (json \ "description").extractOpt[String].flatMap(_.blankOption),
        (json \ "produces").extractOrElse(List.empty[String]),
        (json \ "consumes").extractOrElse(List.empty[String]),
        (json \ "protocols").extractOrElse(List.empty[String]),
        (json \ "apis").extractOrElse(List.empty[Endpoint]),
        (json \ "models").extractOpt[Map[String, Model]].getOrElse(Map.empty),
        (json \ "authorizations").extractOrElse(List.empty[String]),
        (json \ "position").extractOrElse(0)
      )
  }, {
    case x: Api =>
      ("apiVersion" -> x.apiVersion) ~
        ("swaggerVersion" -> x.swaggerVersion) ~
        ("resourcePath" -> x.resourcePath) ~
        ("produces" -> (x.produces match {
          case Nil => JNothing
          case e => Extraction.decompose(e)
        })) ~
        ("consumes" -> (x.consumes match {
          case Nil => JNothing
          case e => Extraction.decompose(e)
        })) ~
        ("protocols" -> (x.protocols match {
          case Nil => JNothing
          case e => Extraction.decompose(e)
        })) ~
        ("authorizations" -> (x.authorizations match {
          case Nil => JNothing
          case e => Extraction.decompose(e)
        })) ~
        ("apis" -> (x.apis match {
          case Nil => JNothing
          case e => Extraction.decompose(e)
        })) ~
        ("models" -> (x.models match {
          case x if x.isEmpty => JNothing
          case e => Extraction.decompose(e)
        }))
  }))

  class GrantTypeSerializer extends CustomSerializer[GrantType](implicit formats => ({
    case value if value \ "type" == JString("implicit") =>
      ImplicitGrant(
        LoginEndpoint((value \ "loginEndpoint" \ "url").as[String]),
        (value \ "tokenName").as[String])
    case value if value \ "type" == JString("authorization_code") =>
      AuthorizationCodeGrant(
        TokenRequestEndpoint(
          (value \ "tokenRequestEndpoint" \ "url").as[String],
          (value \ "tokenRequestEndpoint" \ "clientIdName").as[String],
          (value \ "tokenRequestEndpoint" \ "clientSecretName").as[String]
        ),
        TokenEndpoint(
          (value \ "tokenEndpoint" \ "url").as[String],
          (value \ "tokenEndpoint" \ "tokenName").as[String]))
  }, {
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
  }))

  class AuthorizationTypeSerializer extends CustomSerializer[AuthorizationType](implicit formats => ({
    case value if value \ "type" == JString("apiKey") =>
      ApiKey((value \ "keyname").extractOpt[String].flatMap(_.blankOption).getOrElse("apiKey"), (value \ "passAs").extract[String])
    case value if value \ "type" == JString("oauth2") =>
      OAuth((value \ "scopes").extract[List[String]], (value \ "grantTypes").extract[List[GrantType]])
  }, {
    case obj @ OAuth(scopes, grantTypes) =>
      ("type" -> obj.`type`) ~
        ("scopes" -> scopes) ~
        ("grantTypes" ->
          (for (t <- grantTypes) yield {
            (t.`type`, Extraction.decompose(t))
          }).toMap)
    case obj @ ApiKey(keyname, passAs) =>
      ("type" -> obj.`type`) ~
        ("passAs" -> passAs) ~
        ("keyname" -> keyname)

  }))
}

