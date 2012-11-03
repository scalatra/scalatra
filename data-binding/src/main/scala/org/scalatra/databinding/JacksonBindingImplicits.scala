package org.scalatra
package databinding

import json.{JacksonJsonValueReaderProperty, JacksonJsonSupport}

trait JacksonJsonParsing extends CommandSupport with JacksonJsonValueReaderProperty { self: JacksonJsonSupport with CommandSupport =>
  type CommandType = JsonCommand
//  import JsonZeroes._

  /**
   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  override def command[T <: CommandType](implicit mf: Manifest[T]): T = {
    commandOption[T].getOrElse {
      val newCommand = mf.erasure.newInstance.asInstanceOf[T]
      format match {
        case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
        case _ => newCommand.bindTo(params, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }

  /**
   * Create and bind a [[org.scalatra.databinding.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  override def commandOrElse[T <: CommandType](factory: ⇒ T)(implicit mf: Manifest[T]): T = {
    commandOption[T] getOrElse {
      val newCommand = factory
      format match {
        case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
        case _ => newCommand.bindTo(params, multiParams, request.headers)
      }
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }

}
