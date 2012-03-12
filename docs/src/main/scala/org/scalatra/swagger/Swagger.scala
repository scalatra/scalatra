package org.scalatra
package swagger

/**
 * An instance of this class is used to hold the API documentation.
 */
class Swagger(val swaggerVersion: String, val apiVersion: String) {
  protected var ind: Documentation = null
  protected var docs = Map.empty[String, Documentation]

  /**
   * Returns the index (resource listing).
   */
  def index: Documentation = ind

  /**
   * Returns the documentation for the given path.
   */
  def doc(path: String): Option[Documentation] = docs.get(path)

  /**
   * Registers a documented Scalatra 'app'.
   */
  def register(path: String, s: DocumentationSupport) = {
    val p = "/%s" format path
    docs = docs + (path -> Documentation("", p, swaggerVersion, apiVersion, s.endpoints(p), s.models))
  }

  /**
   * Override this to create a customized index (resource listing).
   */
  def buildIndex() = Documentation("", "", swaggerVersion, apiVersion, docs.values.toList map (doc ⇒ Endpoint(doc.resourcePath + ".json", "", true)), Map.empty)

  /**
   * Builds the index (resource listing) by calling buildIndex.
   */
  def build() = ind = buildIndex()
}
