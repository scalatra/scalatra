package org.scalatra
package swagger

import org.json4s._
import JsonDSL._
import json.JsonSupport

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBaseBase extends Initializable with ScalatraBase { self: JsonSupport[_] with CorsSupport =>

  protected type ApiType <: SwaggerApi[_]


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
   * defaults to true, the format parameter will be present but is still optional.
   * @return true if the format parameter should be included in the returned json
   */
  protected def includeFormatParameter: Boolean = true

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    get("/:doc(.:format)") {
      swagger.doc(params("doc")) match {
        case Some(doc) ⇒ renderDoc(doc.asInstanceOf[ApiType])
        case _         ⇒ halt(404)
      }
    }

    get("/("+indexRoute+"(.:format))") {
      renderIndex(swagger.docs.toList.asInstanceOf[List[ApiType]])
    }

    options("/("+indexRoute+"(.:format))") {}
  }

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected implicit def swagger: SwaggerEngine[_ <: SwaggerApi[_]]
  


  protected def renderDoc(doc: ApiType): JValue = {
    docToJson(doc) merge
      ("basePath" -> fullUrl("/", includeServletPath = false)) ~
      ("swaggerVersion" -> swagger.swaggerVersion) ~
      ("apiVersion" -> swagger.apiVersion)
  }

  protected def renderIndex(docs: List[ApiType]): JValue = {
    ("basePath" -> fullUrl("/", includeServletPath = false)) ~
      ("swaggerVersion" -> swagger.swaggerVersion) ~
      ("apiVersion" -> swagger.apiVersion) ~
      ("apis" ->
        (docs.filter(_.apis.nonEmpty).toList map {
          doc => (("path" -> ((url(doc.resourcePath)) + (if (includeFormatParameter) ".{format}" else ""))) ~
                 ("description" -> doc.description))
        }))
  }

}

trait SwaggerBase extends SwaggerBaseBase with DefaultSwaggerJsonFormats { self: ScalatraBase with JsonSupport[_] with CorsSupport =>
  type ApiType = Api

  protected def docToJson(doc: Api): JValue = Formats.write(doc)
  protected implicit def swagger: SwaggerEngine[ApiType]
}
