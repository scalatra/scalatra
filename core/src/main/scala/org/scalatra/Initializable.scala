package org.scalatra

/**
 * Unifies the initialization process of a Servlet and a Filter.  This is
 * useful for mixins that depend on the ServletConfig/FilterConfig.
 */
trait Initializable {
  def init(config: Config)
}
