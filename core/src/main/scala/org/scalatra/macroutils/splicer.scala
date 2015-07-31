package org.scalatra.macroutils

import scala.language.experimental.macros

case class OrigOwnerAttachment(sym: Any)

import Compat210._

object Splicer {

  import scala.reflect.macros._
  import blackbox.Context

  def impl[A](c: Context)(expr: c.Expr[A]): c.Expr[A] = {
    val helper = new Splicer[c.type](c)
    c.Expr[A](helper.changeOwner(expr.tree))
  }

  class Splicer[C <: Context](val c: C) extends MacrosCompat210 {
    def changeOwner(tree: c.Tree): c.Tree = {
      import c.universe._, internal._, decorators._
      val origOwner = tree.attachments.get[OrigOwnerAttachment].map(_.sym).get.asInstanceOf[Symbol]
      c.internal.changeOwner(tree, origOwner, c.internal.enclosingOwner)
    }
  }

  def changeOwner[A](expr: A): A = macro impl[A]
}
