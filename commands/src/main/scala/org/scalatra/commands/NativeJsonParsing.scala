package org.scalatra
package commands

import json.{ NativeJsonSupport, NativeJsonValueReaderProperty }
import grizzled.slf4j.Logger

trait NativeJsonParsing extends CommandSupport with NativeJsonValueReaderProperty { self: NativeJsonSupport with CommandSupport =>
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