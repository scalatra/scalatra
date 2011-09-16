package org.scalatra
package test.jetty8

import java.util.EnumSet
import javax.servlet.{DispatcherType => ServletDispatcherType, _}
import org.specs2._
import test.DispatcherType
import DispatcherType._
import test.specs2.ScalatraSpec

class AddFilterSpecFilter extends Filter {
  def init(config: FilterConfig) = {}
  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) = {
    res.getWriter.write(req.getDispatcherType.name)
  }
  def destroy() = {}
}

class AddFilterSpec extends ScalatraSpec { def is =
  "Adding a filter instance should"                    ^
    "dispatch on the specified dispatcher type"        ! e1^
    "not dispatch on unspecified dispatcher types"     ! e2^
                                                       p^
  "Adding a filter by class should"                    ^
    "dispatch on the specified dispatcher type"        ! e3^
    "not dispatch on unspecified dispatcher types"     ! e4

  addFilter(new AddFilterSpecFilter, "/instance/request", EnumSet.of(REQUEST))
  addFilter(new AddFilterSpecFilter, "/instance/not-request", EnumSet.complementOf(EnumSet.of(REQUEST)))
  addFilter(classOf[AddFilterSpecFilter], "/class/request", EnumSet.of(REQUEST))
  addFilter(classOf[AddFilterSpecFilter], "/class/not-request", EnumSet.complementOf(EnumSet.of(REQUEST)))

  def e1 = get("/instance/request") {
    body must_== "REQUEST"
  }

  def e2 = get("/instance/not-request") {
    status must_== 404
  }

  def e3 = get("/class/request") {
    body must_== "REQUEST"
  }

  def e4 = get("/class/not-request") {
    status must_== 404
  }
}
