package org.scalatra

import java.util.Enumeration
import javax.servlet.{FilterConfig, ServletContext, ServletConfig}

object Config {
  implicit def servletConfig2Config(servletConfig: ServletConfig) = new Config {
    def getName = servletConfig.getServletName
    def getServletContext = servletConfig.getServletContext
    def getInitParameter(name: String) = servletConfig.getInitParameter(name)
    def getInitParameterNames = servletConfig.getInitParameterNames
  }

  implicit def filterConfig2Config(filterConfig: FilterConfig) = new Config {
    def getName = filterConfig.getFilterName
    def getServletContext = filterConfig.getServletContext
    def getInitParameter(name: String) = filterConfig.getInitParameter(name)
    def getInitParameterNames = filterConfig.getInitParameterNames
  }
}

/**
 * Provides a unified view of a ServletConfig or a FilterConfig.
 */
trait Config {
  def getName: String
  def getServletContext: ServletContext
  def getInitParameter(name: String): String
  def getInitParameterNames: Enumeration[_]
}
