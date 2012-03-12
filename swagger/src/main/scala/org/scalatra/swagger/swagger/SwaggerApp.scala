package org.scalatra
package swagger

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerApp extends ScalatraBase {

  before() {
    contentType = "application/json"
  }

  get("/:doc.json") {
    swag.doc(params("doc")) match {
      case Some(doc) ⇒ Documentation.toJson(doc.copy(basePath = buildFullUrl("")))
      case _         ⇒ halt(404)
    }
  }

  get("/resources.json") {
    Documentation.toJson(swag.index.copy(basePath = buildFullUrl("")))
  }

  options("/resources.json") {}

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected def swag: Swagger

  /**
   * Builds a full URL based on the given path.
   */
  protected def buildFullUrl(path: String): String
}
