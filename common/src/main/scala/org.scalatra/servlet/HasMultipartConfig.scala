package org.scalatra.servlet

import javax.servlet.MultipartConfigElement

trait HasMultiPartConfig {
  protected[scalatra] var multipartConfig: MultiPartConfig = MultiPartConfig()
  def configureMultipartHandling(config: MultiPartConfig) { multipartConfig = config }
}

