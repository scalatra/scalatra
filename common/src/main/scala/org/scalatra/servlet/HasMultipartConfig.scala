package org.scalatra
package servlet

import org.scalatra.ServletCompat.ServletContext

object HasMultipartConfig {
  val DefaultMultipartConfig = MultipartConfig()
  val MultipartConfigKey = "org.scalatra.MultipartConfigKey"
}
trait HasMultipartConfig extends Initializable {

  import org.scalatra.servlet.HasMultipartConfig._

  def servletContext: ServletContext

  private[this] def multipartConfigFromContext: Option[MultipartConfig] =
    // hack to support the tests without changes
    providedConfig orElse {
      try
        (Option(servletContext)
          flatMap (sc => Option(sc.getAttribute(MultipartConfigKey)))
          filterNot (_ == null)
          map (_.asInstanceOf[MultipartConfig]))
      catch {
        case _: NullPointerException => Some(DefaultMultipartConfig)
      }

    }

  def multipartConfig: MultipartConfig = try
    multipartConfigFromContext getOrElse DefaultMultipartConfig
  catch {
    case e: Throwable =>
      System.err.println(
        "Couldn't get the multipart config from the servlet context because: "
      )
      e.printStackTrace()
      DefaultMultipartConfig
  }

  private[this] var providedConfig: Option[MultipartConfig] = None

  abstract override def initialize(config: ConfigT): Unit = {
    super.initialize(config)

    providedConfig foreach { _ apply config.context }
  }

  def configureMultipartHandling(config: MultipartConfig): Unit =
    providedConfig = Some(config)
}
