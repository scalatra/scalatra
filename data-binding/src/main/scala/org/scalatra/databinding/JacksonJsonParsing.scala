package org.scalatra
package databinding

import json.{JacksonJsonValueReaderProperty, JacksonJsonSupport}
import grizzled.slf4j.Logger

trait JacksonJsonParsing extends CommandSupport with JacksonJsonValueReaderProperty { self: JacksonJsonSupport with CommandSupport =>
  type CommandType = JsonCommand

  private[this] val logger: Logger = Logger[this.type]

  override protected def bindCommand[T <: CommandType](newCommand: T)(implicit mf: Manifest[T]): T = {
    logger debug  "binding command: %s from %s".format(mf.erasure.getSimpleName, format)
    format match {
      case "json" | "xml" => newCommand.bindTo(parsedBody, multiParams, request.headers)
      case _ => newCommand.bindTo(params, multiParams, request.headers)
    }
    requestProxy.update(commandRequestKey[T], newCommand)
    newCommand
  }

}
