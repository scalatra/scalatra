package org.scalatra
package commands

import json.{JacksonJsonValueReaderProperty, JacksonJsonSupport}
import grizzled.slf4j.Logger

trait JacksonJsonParsing extends CommandSupport with JacksonJsonValueReaderProperty { self: JacksonJsonSupport with CommandSupport =>
  type CommandType = JsonCommand


  override protected def bindCommand[T <: CommandType](newCommand: T)(implicit mf: Manifest[T]): T = {
    format match {
      case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
      case _ => newCommand.bindTo(params, multiParams, request.headers)
    }
    requestProxy.update(commandRequestKey[T], newCommand)
    newCommand
  }

}
