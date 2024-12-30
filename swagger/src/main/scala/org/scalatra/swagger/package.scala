package org.scalatra

package object swagger {
  object Symbols {
    val Summary       = Symbol("swagger.summary")
    val OperationId   = Symbol("swagger.operationId")
    val ResponseClass = Symbol("swagger.responseClass")
    val Parameters    = Symbol("swagger.parameters")
    val Errors        = Symbol("swagger.errors")
    val Endpoint      = Symbol("swagger.endpoint")
    val Allows        = Symbol("swagger.allows")
    val Operation     = Symbol("swagger.extractOperation")
    val Description   = Symbol("swagger.description")
    val Produces      = Symbol("swagger.produces")
    val Consumes      = Symbol("swagger.consumes")
    val AllSymbols = Set(
      Summary,
      OperationId,
      ResponseClass,
      Parameters,
      Errors,
      Endpoint,
      Allows,
      Operation,
      Description,
      Consumes,
      Produces
    )
  }

  object annotations {
    import scala.annotation.meta.field

    type ApiModelProperty = org.scalatra.swagger.runtime.annotations.ApiModelProperty @field
    type ApiModel         = org.scalatra.swagger.runtime.annotations.ApiModel
  }
}
