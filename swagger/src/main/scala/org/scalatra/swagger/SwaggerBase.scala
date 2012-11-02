package org.scalatra
package swagger

import org.json4s._
import JsonDSL._
import json.JsonSupport

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBaseBase { self: ScalatraBase with JsonSupport[_] with CorsSupport =>

  protected type ApiType <: SwaggerApi[_]
  
  protected def docToJson(doc: ApiType): JValue

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected implicit def swagger: SwaggerEngine[ApiType]
  
  get("/:doc.:format") {
    swagger.doc(params("doc")) match {
      case Some(doc) ⇒ renderDoc(doc)
      case _         ⇒ halt(404)
    }
  }

  get("/resources.:format") {
    renderIndex(swagger.docs.toList)
  }

  options("/resources.:format") {}

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
        (swagger.docs.toList map {
          doc => (("path" -> ((doc.listingPath getOrElse doc.resourcePath) + ".{format}")) ~
                 ("description" -> doc.description))
        }))
  }

}

trait SwaggerBase extends SwaggerBaseBase { self: ScalatraBase with JsonSupport[_] with CorsSupport =>
  type ApiType = Api
  protected def docToJson(doc: Api): JValue = doc.toJValue
}
