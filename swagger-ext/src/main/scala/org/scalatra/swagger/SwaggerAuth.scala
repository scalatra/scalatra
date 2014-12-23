package org.scalatra
package swagger

import java.util.{ Date => JDate }

import grizzled.slf4j.Logger
import org.joda.time._
import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.auth.ScentrySupport
import org.scalatra.json.JsonSupport
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger.SwaggerSerializers.SwaggerFormats
import org.scalatra.util.NotNothing

class SwaggerWithAuth(val swaggerVersion: String, val apiVersion: String, val apiInfo: ApiInfo) extends SwaggerEngine[AuthApi[AnyRef]] {
  private[this] val logger = Logger[this.type]
  /**
   * Registers the documentation for an API with the given path.
   */
  def register(listingPath: String, resourcePath: String, description: Option[String], s: SwaggerSupportSyntax with SwaggerSupportBase, consumes: List[String], produces: List[String], protocols: List[String], authorizations: List[String]) {
    val endpoints: List[AuthEndpoint[AnyRef]] = s.endpoints(resourcePath) collect { case m: AuthEndpoint[AnyRef] => m }
    _docs += listingPath -> AuthApi(
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

import org.scalatra.util.RicherString._

object SwaggerAuthSerializers {
  import org.scalatra.swagger.SwaggerSerializers.{ dontAddOnEmpty, readDataType, writeDataType }

  def authFormats[T <: AnyRef](userOption: Option[T])(implicit mf: Manifest[T]): SwaggerFormats = SwaggerSerializers.formats ++ Seq(
    new AuthOperationSerializer[T](userOption),
    new AuthEndpointSerializer[T],
    new AuthApiSerializer[T]
  )

  class AuthOperationSerializer[T <: AnyRef: Manifest](userOption: Option[T]) extends CustomSerializer[AuthOperation[T]](implicit formats => ({
    case value =>
      AuthOperation[T](
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
    case obj: AuthOperation[T] if obj.allows(userOption) =>
      val json = ("method" -> Extraction.decompose(obj.method)) ~
        ("summary" -> obj.summary) ~
        ("position" -> obj.position) ~
        ("notes" -> obj.notes.flatMap(_.blankOption).getOrElse("")) ~
        ("deprecated" -> obj.deprecated) ~
        ("nickname" -> obj.nickname) ~
        ("parameters" -> Extraction.decompose(obj.parameters)) ~
        ("responseMessages" -> (if (obj.responseMessages.nonEmpty) Some(Extraction.decompose(obj.responseMessages)) else None))

      val consumes = dontAddOnEmpty("consumes", obj.consumes)_
      val produces = dontAddOnEmpty("produces", obj.produces)_
      val protocols = dontAddOnEmpty("protocols", obj.protocols)_
      val authorizations = dontAddOnEmpty("authorizations", obj.authorizations)_
      val r = (consumes andThen produces andThen authorizations andThen protocols)(json)
      r merge writeDataType(obj.responseClass)
    case obj: AuthOperation[_] => JNothing
  }))
  class AuthEndpointSerializer[T <: AnyRef: Manifest] extends CustomSerializer[AuthEndpoint[T]](implicit formats => ({
    case value =>
      AuthEndpoint[T](
        (value \ "path").extract[String],
        (value \ "description").extractOpt[String].flatMap(_.blankOption),
        (value \ "operations").extract[List[AuthOperation[T]]])
  }, {
    case obj: AuthEndpoint[T] =>
      ("path" -> obj.path) ~
        ("description" -> obj.description) ~
        ("operations" -> Extraction.decompose(obj.operations))
  }))
  class AuthApiSerializer[T <: AnyRef: Manifest] extends CustomSerializer[AuthApi[T]](implicit formats => ({
    case json =>
      AuthApi[T](
        (json \ "apiVersion").extractOrElse(""),
        (json \ "swaggerVersion").extractOrElse(""),
        (json \ "resourcePath").extractOrElse(""),
        (json \ "description").extractOpt[String].flatMap(_.blankOption),
        (json \ "produces").extractOrElse(List.empty[String]),
        (json \ "consumes").extractOrElse(List.empty[String]),
        (json \ "protocols").extractOrElse(List.empty[String]),
        (json \ "apis").extractOrElse(List.empty[AuthEndpoint[T]]),
        (json \ "models").extractOpt[Map[String, Model]].getOrElse(Map.empty),
        (json \ "authorizations").extractOrElse(List.empty[String]),
        (json \ "position").extractOrElse(0)
      )
  }, {
    case x: AuthApi[T] =>
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
}

trait SwaggerAuthBase[TypeForUser <: AnyRef] extends SwaggerBaseBase { self: JsonSupport[_] with CorsSupport with ScentrySupport[TypeForUser] =>
  protected type ApiType = AuthApi[TypeForUser]
  protected implicit def swagger: SwaggerEngine[AuthApi[AnyRef]]
  protected def userManifest: Manifest[TypeForUser]
  protected implicit def jsonFormats: Formats = SwaggerAuthSerializers.authFormats(userOption)(userManifest)

  protected def docToJson(doc: ApiType): JValue = Extraction.decompose(doc)
  before() {
    scentry.authenticate()
  }

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)

    get("/:doc(.:format)") {
      def isAllowed(doc: AuthApi[AnyRef]) = doc.apis.exists(_.operations.exists(_.allows(userOption)))
      swagger.doc(params("doc")) match {
        case Some(doc) if isAllowed(doc) ⇒ renderDoc(doc.asInstanceOf[ApiType])
        case _ ⇒ NotFound()
      }
    }

    get("/" + indexRoute + "(.:format)") {
      val docs = swagger.docs.filter(_.apis.exists(_.operations.exists(_.allows(userOption)))).toList
      if (docs.isEmpty) halt(NotFound())
      renderIndex(docs.asInstanceOf[List[ApiType]])
    }
  }

  protected override def renderIndex(docs: List[ApiType]): JValue = {
    ("apiVersion" -> swagger.apiVersion) ~
      ("swaggerVersion" -> swagger.swaggerVersion) ~
      ("apis" ->
        (docs.filter(s => s.apis.nonEmpty && s.apis.exists(_.operations.exists(_.allows(userOption)))).toList map {
          doc =>
            ("path" -> (url(doc.resourcePath, includeServletPath = false, includeContextPath = false) + (if (includeFormatParameter) ".{format}" else ""))) ~
              ("description" -> doc.description)
        })) ~
        ("authorizations" -> swagger.authorizations.foldLeft(JObject(Nil)) { (acc, auth) =>
          acc merge JObject(List(auth.`type` -> Extraction.decompose(auth)(SwaggerAuthSerializers.authFormats(userOption)(userManifest))))
        }) ~
        ("info" -> Option(swagger.apiInfo).map(Extraction.decompose(_)(SwaggerAuthSerializers.authFormats(userOption)(userManifest))))
  }

}

case class AuthApi[TypeForUser <: AnyRef](
  apiVersion: String,
  swaggerVersion: String,
  resourcePath: String,
  description: Option[String] = None,
  produces: List[String] = Nil,
  consumes: List[String] = Nil,
  protocols: List[String] = Nil,
  apis: List[AuthEndpoint[TypeForUser]] = Nil,
  models: Map[String, Model] = Map.empty,
  authorizations: List[String] = Nil,
  position: Int = 0) extends SwaggerApi[AuthEndpoint[TypeForUser]]
object AuthApi {

  import org.scalatra.swagger.SwaggerSupportSyntax.SwaggerOperationBuilder

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  trait SwaggerAuthOperationBuilder[T <: AnyRef] extends SwaggerOperationBuilder[AuthOperation[T]] {
    private[this] var _allows: Option[T] => Boolean = (u: Option[T]) => true
    def allows: Option[T] => Boolean = _allows
    def allows(guard: Option[T] => Boolean): this.type = { _allows = guard; this }
    def allowAll: this.type = { _allows = (u: Option[T]) => true; this }
  }

  class AuthOperationBuilder[T <: AnyRef](val resultClass: DataType) extends SwaggerAuthOperationBuilder[T] {
    def result: AuthOperation[T] = AuthOperation[T](
      null,
      resultClass,
      summary,
      position,
      notes,
      deprecated,
      nickname,
      parameters,
      responseMessages,
      consumes,
      produces,
      protocols,
      authorizations,
      allows
    )
  }

}

case class AuthEndpoint[TypeForUser <: AnyRef](path: String,
  description: Option[String] = None,
  operations: List[AuthOperation[TypeForUser]] = Nil) extends SwaggerEndpoint[AuthOperation[TypeForUser]]

case class AuthOperation[TypeForUser <: AnyRef](method: HttpMethod,
  responseClass: DataType,
  summary: String,
  position: Int,
  notes: Option[String] = None,
  deprecated: Boolean = false,
  nickname: Option[String] = None,
  parameters: List[Parameter] = Nil,
  responseMessages: List[ResponseMessage[_]] = Nil,
  consumes: List[String] = Nil,
  produces: List[String] = Nil,
  protocols: List[String] = Nil,
  authorizations: List[String] = Nil,
  allows: Option[TypeForUser] => Boolean = (_: Option[TypeForUser]) => true) extends SwaggerOperation

trait SwaggerAuthSupport[TypeForUser <: AnyRef] extends SwaggerSupportBase with SwaggerSupportSyntax { self: ScalatraBase with ScentrySupport[TypeForUser] =>
  import org.scalatra.swagger.AuthApi.AuthOperationBuilder

  @deprecated("Use the `apiOperation.allows` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def allows(value: Option[TypeForUser] => Boolean) = swaggerMeta(Symbols.Allows, value)

  private def allowAll = (u: Option[TypeForUser]) => true

  protected implicit def operationBuilder2operation(bldr: AuthApi.SwaggerAuthOperationBuilder[TypeForUser]): AuthOperation[TypeForUser] =
    bldr.result

  protected def apiOperation[T: Manifest: NotNothing](nickname: String): AuthOperationBuilder[TypeForUser] = {
    registerModel[T]()
    new AuthOperationBuilder[TypeForUser](DataType[T]).nickname(nickname).errors(swaggerDefaultErrors: _*)
  }

  protected def apiOperation(nickname: String, model: Model): AuthOperationBuilder[TypeForUser] = {
    registerModel(model)
    new AuthOperationBuilder[TypeForUser](ValueDataType(model.id)).nickname(nickname).errors(swaggerDefaultErrors: _*)
  }

  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[AuthEndpoint[TypeForUser]] = {
    (swaggerEndpointEntries(extractOperation) groupBy (_.key)).toList map {
      case (name, entries) =>
        val desc = _description.lift apply name
        val pth = if (basePath endsWith "/") basePath else basePath + "/"
        val nm = if (name startsWith "/") name.substring(1) else name
        new AuthEndpoint[TypeForUser](pth + nm, desc, entries.toList map (_.value))
    } sortBy (_.path)
  }
  /**
   * Returns a list of operations based on the given route. The default implementation returns a list with only 1
   * operation.
   */
  protected def extractOperation(route: Route, method: HttpMethod): AuthOperation[TypeForUser] = {
    val op = route.metadata.get(Symbols.Operation) map (_.asInstanceOf[AuthOperation[TypeForUser]])
    op map (_.copy(method = method)) getOrElse {
      val theParams = route.metadata.get(Symbols.Parameters) map (_.asInstanceOf[List[Parameter]]) getOrElse Nil
      val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[ResponseMessage[_]]]) getOrElse Nil
      val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[DataType]) getOrElse DataType.Void
      val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
      val notes = route.metadata.get(Symbols.Notes) map (_.asInstanceOf[String])
      val nick = route.metadata.get(Symbols.Nickname) map (_.asInstanceOf[String])
      val produces = route.metadata.get(Symbols.Produces) map (_.asInstanceOf[List[String]]) getOrElse Nil
      val allows = route.metadata.get(Symbols.Allows) map (_.asInstanceOf[Option[TypeForUser] => Boolean]) getOrElse allowAll
      val consumes = route.metadata.get(Symbols.Consumes) map (_.asInstanceOf[List[String]]) getOrElse Nil
      AuthOperation[TypeForUser](
        method = method,
        responseClass = responseClass,
        summary = summary,
        position = 0,
        notes = notes,
        nickname = nick,
        parameters = theParams,
        responseMessages = (errors ::: swaggerDefaultMessages ::: swaggerDefaultErrors).distinct,
        produces = produces,
        consumes = consumes,
        allows = allows)
    }
  }

}
