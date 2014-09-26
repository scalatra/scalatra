package org.scalatra
package commands

import json.{ NativeJsonSupport, NativeJsonValueReaderProperty }
import grizzled.slf4j.Logger
import javax.servlet.http.HttpServletRequest
import scala.reflect.ClassTag

trait NativeJsonParsing extends CommandSupport with NativeJsonValueReaderProperty { self: NativeJsonSupport with CommandSupport =>
  type CommandType = JsonCommand

  override protected def bindCommand[T <: CommandType](newCommand: T)(implicit request: HttpServletRequest, ct: ClassTag[T]): T = {
    format match {
      case "json" | "xml" => newCommand.bindTo(parsedBody(request), multiParams(request), request.headers)
      case _ => newCommand.bindTo(params(request), multiParams(request), request.headers)
    }
    request.update(commandRequestKey[T], newCommand)
    newCommand
  }

}