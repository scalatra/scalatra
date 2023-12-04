package org.scalatra.servlet

import jakarta.servlet.{ MultipartConfigElement, ServletContext }

case class MultipartConfig(
  location: Option[String] = None,
  maxFileSize: Option[Long] = None,
  maxRequestSize: Option[Long] = None,
  fileSizeThreshold: Option[Int] = None) extends MountConfig {

  def toMultipartConfigElement = {
    new MultipartConfigElement(
      location.getOrElse(""),
      maxFileSize.getOrElse(-1),
      maxRequestSize.getOrElse(-1),
      fileSizeThreshold.getOrElse(0))
  }

  def apply(ctxt: ServletContext): Unit = {
    ctxt.setAttribute(HasMultipartConfig.MultipartConfigKey, this)
  }
}
