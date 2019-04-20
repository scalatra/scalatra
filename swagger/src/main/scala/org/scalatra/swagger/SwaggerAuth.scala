package org.scalatra
package swagger

import org.json4s._
import org.scalatra.auth.ScentrySupport
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.util.NotNothing

class SwaggerWithAuth(val swaggerVersion: String, val apiVersion: String, val apiInfo: ApiInfo) extends SwaggerEngine[AuthApi[AnyRef]] {

  /**
   * Registers the documentation for an API with the given path.
   */
  def register(listingPath: String, resourcePath: String, description: Option[String], s: SwaggerSupportSyntax with SwaggerSupportBase, consumes: List[String], produces: List[String], protocols: List[String], authorizations: List[String]): Unit = {
    val endpoints: List[AuthEndpoint[AnyRef]] = s.endpoints(resourcePath) collect { case m: AuthEndpoint[AnyRef] => m }
    _docs += listingPath -> AuthApi(
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
      0)
  }
}

trait SwaggerAuthBase[TypeForUser <: AnyRef] extends SwaggerBaseBase { self: CorsSupport with ScentrySupport[TypeForUser] =>

  protected type ApiType = AuthApi[TypeForUser]
  protected implicit def swagger: SwaggerEngine[AuthApi[AnyRef]]
  protected def userManifest: Manifest[TypeForUser]
  protected implicit def jsonFormats: Formats = DefaultFormats

  protected def docToJson(doc: ApiType): JValue = Extraction.decompose(doc)
  before() {
    scentry.authenticate()
  }

  abstract override def initialize(config: ConfigT): Unit = {
    super.initialize(config)
    get("/swagger.json") {
      val docs = filterDocs(swagger.docs)
      if (docs.isEmpty) halt(NotFound())
      renderSwagger2(docs.asInstanceOf[List[ApiType]])
    }
  }

  protected def filterDoc(doc: AuthApi[AnyRef]): AuthApi[AnyRef] = {
    doc.copy(apis = doc.apis.collect {
      case api if api.operations.exists(_.allows(userOption)) =>
        api.copy(operations = api.operations.filter(_.allows(userOption)))
    })
  }

  protected def filterDocs(docs: Iterable[AuthApi[AnyRef]]): Iterable[AuthApi[AnyRef]] = {
    docs.collect {
      case doc if doc.apis.exists(_.operations.exists(_.allows(userOption))) =>
        filterDoc(doc)
    }.toList
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

  trait SwaggerAuthOperationBuilder[T <: AnyRef] extends SwaggerOperationBuilder[AuthOperation[T]] {
    private[this] var _allows: Option[T] => Boolean = (u: Option[T]) => true
    def allows: Option[T] => Boolean = _allows
    def allows(guard: Option[T] => Boolean): this.type = { _allows = guard; this }
    def allowAll: this.type = { _allows = (u: Option[T]) => true; this }
  }

  class AuthOperationBuilder[T <: AnyRef](val resultClass: DataType) extends SwaggerAuthOperationBuilder[T] {
    def result: AuthOperation[T] = AuthOperation[T](
      null,
      operationId,
      resultClass,
      summary,
      position,
      description,
      deprecated,
      parameters,
      responseMessages,
      consumes,
      produces,
      schemes,
      authorizations,
      tags,
      allows)
  }

}

case class AuthEndpoint[TypeForUser <: AnyRef](
  path: String,
  description: Option[String] = None,
  operations: List[AuthOperation[TypeForUser]] = Nil) extends SwaggerEndpoint[AuthOperation[TypeForUser]]

case class AuthOperation[TypeForUser <: AnyRef](
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
  tags: List[String] = Nil,
  allows: Option[TypeForUser] => Boolean = (_: Option[TypeForUser]) => true) extends SwaggerOperation

trait SwaggerAuthSupport[TypeForUser <: AnyRef] extends SwaggerSupportBase with SwaggerSupportSyntax { self: ScalatraBase with ScentrySupport[TypeForUser] =>
  import org.scalatra.swagger.AuthApi.AuthOperationBuilder

  private def allowAll = (u: Option[TypeForUser]) => true

  protected implicit def operationBuilder2operation(bldr: AuthApi.SwaggerAuthOperationBuilder[TypeForUser]): AuthOperation[TypeForUser] =
    bldr.result

  protected def apiOperation[T: Manifest: NotNothing](operationId: String): AuthOperationBuilder[TypeForUser] = {
    registerModel[T]()
    new AuthOperationBuilder[TypeForUser](DataType[T]).operationId(operationId)
  }

  protected def apiOperation(operationId: String, model: Model): AuthOperationBuilder[TypeForUser] = {
    registerModel(model)
    new AuthOperationBuilder[TypeForUser](ValueDataType(model.id)).operationId(operationId)
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
      val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[ResponseMessage]]) getOrElse Nil
      val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[DataType]) getOrElse DataType.Void
      val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
      val description = route.metadata.get(Symbols.Description) map (_.asInstanceOf[String])
      val operationId = route.metadata.get(Symbols.OperationId) map (_.asInstanceOf[String]) getOrElse ""
      val produces = route.metadata.get(Symbols.Produces) map (_.asInstanceOf[List[String]]) getOrElse Nil
      val allows = route.metadata.get(Symbols.Allows) map (_.asInstanceOf[Option[TypeForUser] => Boolean]) getOrElse allowAll
      val consumes = route.metadata.get(Symbols.Consumes) map (_.asInstanceOf[List[String]]) getOrElse Nil
      AuthOperation[TypeForUser](
        method = method,
        responseClass = responseClass,
        summary = summary,
        position = 0,
        description = description,
        operationId = operationId,
        parameters = theParams,
        responseMessages = (errors ::: swaggerDefaultMessages).distinct,
        produces = produces,
        consumes = consumes,
        allows = allows)
    }
  }

}
