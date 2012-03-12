package org.scalatra

package object swagger {
  object Symbols {
    lazy val Summary = Symbol("swagger.summary")
    lazy val Notes = Symbol("swagger.notes")
    lazy val Nickname = Symbol("swagger.nickname")
    lazy val ResponseClass = Symbol("swagger.responseClass")
    lazy val Parameters = Symbol("swagger.parameters")
    lazy val Endpoint = Symbol("swagger.endpoint")
  }
}