package org.scalatra

import scala.reflect.macros.Context
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

package object macros {
  def requestImpl(c: Context): c.Expr[HttpServletRequest] = {
    c.universe.reify { null: HttpServletRequest }
  }

  def responseImpl(c: Context): c.Expr[HttpServletResponse] = {
    c.universe.reify { null: HttpServletResponse }
  }
}
