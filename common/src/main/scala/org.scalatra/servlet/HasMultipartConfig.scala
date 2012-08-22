package org.scalatra.servlet

import javax.servlet.MultipartConfigElement

trait HasMultipartConfig {
  protected[scalatra] var multipartConfig: MultipartConfig = MultipartConfig()
  def configureMultipartHandling(config: MultipartConfig) { multipartConfig = config }
}

