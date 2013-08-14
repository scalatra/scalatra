package org.scalatra
package swagger

import java.util.{Date => JDate, Locale}
import org.json4s._
import org.json4s.JsonDSL._
import ext.{ JodaTimeSerializers, EnumNameSerializer }
import org.joda.time._
import format.ISODateTimeFormat
import grizzled.slf4j.Logger
import org.scalatra.json.JsonSupport
import org.scalatra.auth.ScentrySupport
import collection.mutable
import com.wordnik.swagger.model.LoginEndpoint
import com.wordnik.swagger.model.TokenRequestEndpoint
import com.wordnik.swagger.model.TokenEndpoint

class SwaggerWithAuth(val swaggerVersion: String, val apiVersion: String) extends SwaggerEngine[AuthApi[AnyRef]] {
  private[this] val logger = Logger[this.type]
  /**
   * Registers the documentation for an API with the given path.
   */
  def register(name: String, path: String, description: String, s: SwaggerSupportSyntax with SwaggerSupportBase, listingPath: Option[String] = None) = {
    logger.debug("registering swagger api with: { name: %s, path: %s, description: %s, servlet: %s, listingPath: %s }" format (name, path, description, s.getClass, listingPath))
    _docs += name -> AuthApi(path, listingPath, description, s.endpoints(path).map(_.asInstanceOf[AuthEndpoint[AnyRef]]), s.models.toMap)
  }
}

trait SwaggerAuthBase[TypeForUser <: AnyRef] extends SwaggerBaseBase { self: JsonSupport[_] with CorsSupport with ScentrySupport[TypeForUser] =>
  protected type ApiType = AuthApi[TypeForUser]
  protected implicit def swagger: SwaggerEngine[AuthApi[AnyRef]]
  protected def docToJson(doc: ApiType): JValue = doc.toJValue(userOption)
  
  before() {
    scentry.authenticate()    
  }

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)

    get("/:doc(.:format)") {
      def isAllowed(doc: AuthApi[AnyRef]) = doc.apis.exists(_.operations.exists(_.allows(userOption)))
      swagger.doc(params("doc")) match {
        case Some(doc) if isAllowed(doc) ⇒ renderDoc(doc.asInstanceOf[ApiType])
        case _         ⇒ NotFound()
      }
    }

    get("/"+indexRoute+"(.:format)") {
      val docs = swagger.docs.filter(_.apis.exists(_.operations.exists(_.allows(userOption)))).toList
      if (docs.isEmpty) halt(NotFound())
      renderIndex(docs.asInstanceOf[List[ApiType]])
    }
  }

}

case class AuthApi[TypeForUser <: AnyRef](resourcePath: String,
               listingPath: Option[String],
               description: String,
               apis: List[AuthEndpoint[TypeForUser]],
               models: Map[String, Model],
               info: Option[ApiInfo] = None) extends SwaggerApi[AuthEndpoint[TypeForUser]] {
  def toJValue[T >: TypeForUser <: TypeForUser](userOption: Option[T]) = AuthApi.toJValue(this, userOption)
}

object AuthApi {
  import SwaggerSerializers._
  import SwaggerSupportSyntax.SwaggerOperationBuilder

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  trait SwaggerAuthOperationBuilder[T <: AnyRef] extends SwaggerOperationBuilder[AuthOperation[T]] {
    private[this] var _allows: Option[T] => Boolean = (u: Option[T]) => true
    def allows: Option[T] => Boolean = _allows
    def allows(guard: Option[T] => Boolean): this.type = { _allows = guard; this }
    def allowAll: this.type = { _allows = (u: Option[T]) => true; this }
  }


  class AuthOperationBuilder[T <: AnyRef](val resultClass: String) extends SwaggerAuthOperationBuilder[T] {
    def result: AuthOperation[T] = AuthOperation[T](
      null,
      resultClass,
      summary,
      notes,
      deprecated,
      nickname,
      parameters,
      errorResponses,
      allows
    )
  }

  private[this] def formats[T <: AnyRef](userOption: Option[T]) = new DefaultFormats {
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
    new ModelFieldSerializer,
    new AuthOperationSerializer(userOption)) ++ JodaTimeSerializers.all

  def toJValue[T <: AnyRef](doc: Any, uOpt: Option[T]) = Extraction.decompose(doc)(formats(uOpt))

  class AuthOperationSerializer[T <: AnyRef](userOption: Option[T]) extends Serializer[AuthOperation[T]] {

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, _root_.org.json4s.JValue), AuthOperation[T]] = {
      case _ => null
    }

    def serialize(implicit format: Formats): PartialFunction[Any, _root_.org.json4s.JValue] = {
      case x: AuthOperation[T] if x.allows(userOption) =>        
        import Extraction.decompose
        ("method" -> decompose(x.method)) ~
        ("responseClass" -> x.responseClass) ~
        ("summary" -> x.summary) ~
        ("notes" -> x.notes) ~
        ("deprecated" -> x.deprecated) ~
        ("nickname" -> x.nickname) ~
        ("parameters" -> x.parameters.map(decompose)) ~
        ("responseMessages" -> x.errorResponses.map(decompose))
      case x: AuthOperation[_] => JNothing
    }
  }
}

case class AuthEndpoint[TypeForUser <: AnyRef](path: String,
                                               description: String,
                                               secured: Boolean = false,
                                               operations: List[AuthOperation[TypeForUser]] = Nil) extends SwaggerEndpoint[AuthOperation[TypeForUser]]

case class AuthOperation[TypeForUser <: AnyRef](method: HttpMethod,
                                                responseClass: String,
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
												                        allows: Option[TypeForUser] => Boolean = (_: Option[TypeForUser]) => true,
                                                supportedContentTypes: List[String] = Nil) extends SwaggerOperation

trait SwaggerAuthSupport[TypeForUser <: AnyRef] extends SwaggerSupportBase with SwaggerSupportSyntax { self: ScalatraBase with ScentrySupport[TypeForUser] =>
  import AuthApi.AuthOperationBuilder
  import SwaggerSupportSyntax._

  @deprecated("Use the `apiOperation.allows` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def allows(value: Option[TypeForUser] => Boolean) = swaggerMeta(Symbols.Allows, value)
  
  private def allowAll = (u: Option[TypeForUser]) => true

  protected implicit def operationBuilder2operation(bldr: AuthApi.SwaggerAuthOperationBuilder[TypeForUser]): AuthOperation[TypeForUser] =
    bldr.result

  protected def apiOperation[T](nickname: String)(implicit mf: Manifest[T]): AuthOperationBuilder[TypeForUser] = {
    registerModel[T]()
    new AuthOperationBuilder[TypeForUser](DataType[T].name).nickname(nickname).errors(swaggerDefaultErrors:_*)
  }


  protected def apiOperation(nickname: String, model: Model): AuthOperationBuilder[TypeForUser] = {
    registerModel(model)
    new AuthOperationBuilder[TypeForUser](model.id).nickname(nickname).errors(swaggerDefaultErrors:_*)
  }


  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[AuthEndpoint[TypeForUser]] = {

    (swaggerEndpointEntries(extractOperation) groupBy (_.key)).toList map { case (name, entries) =>
      val sec = entries.exists(!_.value.allows(None))
      val desc = _description.lift apply name getOrElse ""
      val pth = if (basePath endsWith "/") basePath else basePath + "/"
      val nm = if (name startsWith "/") name.substring(1) else name
      new AuthEndpoint[TypeForUser](pth + nm, desc, sec, entries.toList map (_.value))
    } sortBy (_.path)
  }
  /**
   * Returns a list of operations based on the given route. The default implementation returns a list with only 1
   * operation.
   */
  protected def extractOperation(route: Route, method: HttpMethod): AuthOperation[TypeForUser] = {
    val op = route.metadata.get(Symbols.Operation) map (_.asInstanceOf[AuthOperation[TypeForUser]])
    op map (_.copy(httpMethod = method)) getOrElse {
      val theParams = route.metadata.get(Symbols.Parameters) map (_.asInstanceOf[List[Parameter]]) getOrElse Nil
      val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[Error]]) getOrElse Nil
      val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[String]) getOrElse DataType.Void.name
      val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
      val notes = route.metadata.get(Symbols.Notes) map (_.asInstanceOf[String])
      val nick = route.metadata.get(Symbols.Nickname) map (_.asInstanceOf[String])
      val allows = route.metadata.get(Symbols.Allows) map (_.asInstanceOf[Option[TypeForUser] => Boolean]) getOrElse allowAll
      AuthOperation[TypeForUser](
        httpMethod = method,
        responseClass = responseClass,
        summary = summary,
        notes = notes,
        nickname = nick,
        parameters = theParams,
        errorResponses = errors ::: swaggerDefaultErrors,
        allows = allows)
    }
  }

}
