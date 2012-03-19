package org.scalatra
package swagger

import net.liftweb.json._
import JsonDSL._

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBase extends ScalatraBase {

  before() {
    contentType = "application/json"
  }

  get("/:doc.json") {
    swagger.doc(params("doc")) match {
      case Some(doc) ⇒ renderDoc(doc)
      case _         ⇒ halt(404)
    }
  }

  get("/resources.json") {
    renderIndex(swagger.docs.toList)
  }

  options("/resources.json") {}

  protected def renderDoc(doc: Api) = {
    val j = Api.toJObject(doc) ~ ("basePath" -> buildFullUrl("")) ~ ("swaggerVersion" -> swagger.swaggerVersion) ~ ("apiVersion" -> swagger.apiVersion)
    Printer.compact(JsonAST.render(j))
  }

  protected def renderIndex(docs: List[Api]) = {
    val j = ("basePath" -> buildFullUrl("")) ~ ("swaggerVersion" -> swagger.swaggerVersion) ~ ("apiVersion" -> swagger.apiVersion) ~
      ("apis" -> (swagger.docs.toList map (doc => (("path" -> (doc.resourcePath + ".{format}")) ~ ("description" -> doc.description)))))
    Printer.compact(JsonAST.render(j))
  }

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected def swagger: Swagger

  /**
   * Builds a full URL based on the given path.
   */
  protected def buildFullUrl(path: String): String
}
