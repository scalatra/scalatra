package org.scalatra.servlet

import javax.servlet.MultipartConfigElement

case class MultipartConfig(
                            location: Option[String] = None,
                            maxFileSize: Option[Long] = None,
                            maxRequestSize: Option[Long] = None,
                            fileSizeThreshold: Option[Int] = None
                            ) {

  def toMultipartConfigElement = {
    new MultipartConfigElement(
      location.getOrElse(""),
      maxFileSize.getOrElse(-1),
      maxRequestSize.getOrElse(-1),
      fileSizeThreshold.getOrElse(0))
  }
}
