package org.scalatra
package swagger

import org.json4s._
import JsonDSL._
import json.JsonSupport
import grizzled.slf4j.Logger
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBaseBase extends Initializable with ScalatraBase { self: JsonSupport[_] with CorsSupport =>

  protected type ApiType <: SwaggerApi[_]

  protected implicit def jsonFormats: Formats
  protected def docToJson(doc: ApiType): JValue

  implicit override def string2RouteMatcher(path: String) = new RailsRouteMatcher(path)

  /**
   * The name of the route to use when getting the index listing for swagger
   * defaults to optional resources.:format or /
   * @return The name of the route
   */
  protected def indexRoute: String = "resources"

  /**
   * Whether to include the format parameter in the index listing for swagger
   * defaults to false, the format parameter will not be present but is still optional.
   * @return true if the format parameter should be included in the returned json
   */
  protected def includeFormatParameter: Boolean = false

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    addRoute(Get, Seq("""/([^.]+)*(?:\.(\w+))?""".r), (req, resp) => {
      val doc :: fmt :: Nil = multiParams("captures")(req).toList
      if (fmt != null) format_=(fmt)(req)
      swagger.doc(doc) match {
        case Some(doc) ⇒ renderDoc(doc.asInstanceOf[ApiType], req, resp)
        case _         ⇒ halt(404)
      }
    })

    addRoute(Get, Seq("/("+indexRoute+"(.:format))"), (req, resp) => {
      renderIndex(swagger.docs.toList.asInstanceOf[List[ApiType]])(req, resp)
    })

    addRoute(Options, Seq("/("+indexRoute+"(.:format))") , (_, _) => ())
  }

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected implicit def swagger: SwaggerEngine[_ <: SwaggerApi[_]]

  protected def renderDoc(doc: ApiType, req: HttpServletRequest, resp: HttpServletResponse): JValue = {
    val json = docToJson(doc) merge
      ("basePath" -> fullUrl("/", includeServletPath = false)(req, resp)) ~
      ("swaggerVersion" -> swagger.swaggerVersion) ~
      ("apiVersion" -> swagger.apiVersion)
    val consumes = dontAddOnEmpty("consumes", doc.consumes)_
    val produces = dontAddOnEmpty("produces", doc.produces)_
    val protocols = dontAddOnEmpty("protocols", doc.protocols)_
    val authorizations = dontAddOnEmpty("authorizations", doc.authorizations)_
    val jsonDoc = (consumes andThen produces andThen protocols andThen authorizations)(json)
//    println("The rendered json doc:\n" + jackson.prettyJson(jsonDoc))
    jsonDoc
  }

  private[this] def dontAddOnEmpty(key: String, value: List[String])(json: JValue) = {
    val v: JValue = if (value.nonEmpty) key -> value else JNothing
    json merge v
  }

  protected def renderIndex(docs: List[ApiType])(implicit req: HttpServletRequest, resp: HttpServletResponse): JValue = {
    ("apiVersion" -> swagger.apiVersion) ~
    ("swaggerVersion" -> swagger.swaggerVersion) ~
    ("apis" ->
      (docs.filter(_.apis.nonEmpty).toList map {
        doc =>
          ("path" -> (url(doc.resourcePath, includeServletPath = false, includeContextPath = false) + (if (includeFormatParameter) ".{format}" else ""))) ~
          ("description" -> doc.description)
      })) ~
    ("authorizations" -> swagger.authorizations.foldLeft(JObject(Nil)) { (acc, auth) =>
      acc merge JObject(List(auth.`type` -> Extraction.decompose(auth)))
    }) ~
    ("info" -> Option(swagger.apiInfo).map(Extraction.decompose(_)))
  }

  errorAction( (_, _) => {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  })

}

trait SwaggerBase extends SwaggerBaseBase { self: ScalatraBase with JsonSupport[_] with CorsSupport =>
  type ApiType = Api
  implicit protected def jsonFormats: Formats = SwaggerSerializers.defaultFormats
  protected def docToJson(doc: Api): JValue = Extraction.decompose(doc)
  protected implicit def swagger: SwaggerEngine[ApiType]
}
