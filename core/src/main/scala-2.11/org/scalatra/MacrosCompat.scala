package org.scalatra

trait MacrosCompat extends Internal210 {

  type Context = scala.reflect.macros.blackbox.Context

  def freshName(name: String): String = c.freshName(name)

  def typeName(name: String): c.universe.TypeName  = c.universe.TypeName(name)

  def termName(name: String): c.universe.TermName = c.universe.TermName(name)

  def typecheck(tree: c.universe.Tree): c.universe.Tree = c.typecheck(tree)

  def untypecheck(tree: c.universe.Tree): c.universe.Tree = c.untypecheck(tree)

}
