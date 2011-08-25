package org.scalatra
package scalate

/**
 * A ScalateHelper provides following syntax sugars for renderTemplate.
 * `jade`, `scaml`, `ssp`, `mustache`
 */
trait ScalateHelper {
  this: ScalateSupport =>

  lazy val defaultIndexName    = "index"
  lazy val defaultFormat       = "scaml"
  lazy val defaultTemplatePath = "/WEB-INF/scalate/templates"

  /**
   * Syntax sugars for various scalate formats
   */
  def jade     = renderTemplateAs("jade") _
  def scaml    = renderTemplateAs("scaml") _
  def ssp      = renderTemplateAs("ssp") _
  def mustache = renderTemplateAs("mustache") _

  protected def renderTemplateAs(ext: String)(path: String, attributes: (String, Any)*) =
    templateEngine.layout(findTemplate(path, ext), Map(attributes : _*))

  /**
   * Return a template path with WEB-INF prefix.
   */
  protected def findTemplate(name: String, ext: String = defaultFormat) =
    defaultTemplatePath + "/" + completeTemplateName(name, ext)

  /**
   * Complate template name about default index and extname
   */
  protected def completeTemplateName(name: String, ext: String) = {
    val base = name match {
      case s if s.endsWith("/") => s + defaultIndexName
      case s => s
    }
    base match {
      case s if s.contains(".") => s
      case s => s + "." + ext
    }
  }
}
