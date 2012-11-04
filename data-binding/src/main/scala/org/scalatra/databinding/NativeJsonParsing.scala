package org.scalatra
package databinding

import json.{ NativeJsonSupport, NativeJsonValueReaderProperty }

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