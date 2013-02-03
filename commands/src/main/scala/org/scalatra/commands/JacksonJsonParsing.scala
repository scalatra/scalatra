package org.scalatra
package commands

import json.{JacksonJsonValueReaderProperty, JacksonJsonSupport}
import grizzled.slf4j.Logger
import javax.servlet.http.HttpServletRequest

trait JacksonJsonParsing extends CommandSupport with JacksonJsonValueReaderProperty { self: JacksonJsonSupport with CommandSupport =>
  type CommandType = JsonCommand


  override protected def bindCommand[T <: CommandType](newCommand: T)(implicit request: HttpServletRequest, mf: Manifest[T]): T = {
    format match {
      case "json" | "xml" => newCommand.bindTo(parsedBody(request), multiParams(request), request.headers)
      case _ => newCommand.bindTo(params(request), multiParams(request), request.headers)
    }
    request.update(commandRequestKey[T], newCommand)
    newCommand
  }

}
