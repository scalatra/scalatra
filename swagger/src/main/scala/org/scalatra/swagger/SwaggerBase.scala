package org.scalatra
package swagger

import org.json4s._
import JsonDSL._
import json.JsonSupport

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBase { self: ScalatraBase with JsonSupport[_] with CorsSupport =>

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

  protected def renderDoc(doc: Api): JValue = {
    doc.toJValue merge ("basePath" -> fullUrl("")) ~ ("swaggerVersion" -> swagger.swaggerVersion) ~ ("apiVersion" -> swagger.apiVersion)
  }

  protected def renderIndex(docs: List[Api]): JValue = {
    ("basePath" -> fullUrl("")) ~ ("swaggerVersion" -> swagger.swaggerVersion) ~ ("apiVersion" -> swagger.apiVersion) ~
      ("apis" -> (swagger.docs.toList map (doc => (("path" -> (doc.resourcePath + ".{format}")) ~ ("description" -> doc.description)))))
  }

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected implicit def swagger: Swagger

}
