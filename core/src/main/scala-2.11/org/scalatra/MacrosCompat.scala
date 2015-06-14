package org.scalatra

object MacrosCompat {

  type Context = scala.reflect.macros.blackbox.Context

  def freshName[C <: Context](c: C)(name: String): String = c.freshName(name)

  def typeName[C <: Context](c: C)(name: String): c.universe.TypeName  = c.universe.TypeName(name)

  def termName[C <: Context](c: C)(name: String): c.universe.TermName = c.universe.TermName(name)

  def typecheck[C <: Context](c: C)(tree: c.universe.Tree): c.universe.Tree = c.typecheck(tree)

  def untypecheck[C <: Context](c: C)(tree: c.universe.Tree): c.universe.Tree = c.untypecheck(tree)

}
