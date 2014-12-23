package org.scalatra
package commands

import javax.servlet.http.HttpServletRequest

import org.scalatra.json.{ JacksonJsonSupport, JacksonJsonValueReaderProperty }

trait JacksonJsonParsing extends CommandSupport with JacksonJsonValueReaderProperty { self: JacksonJsonSupport with CommandSupport =>
  type CommandType = JsonCommand

  override protected def bindCommand[T <: CommandType](newCommand: T)(implicit request: HttpServletRequest, mf: Manifest[T]): T = {
    val requestFormat = request.contentType match {
      case Some(contentType) => mimeTypes.getOrElse(contentType, format)
      case None => format
    }

    requestFormat match {
      case "json" | "xml" => newCommand.bindTo(parsedBody(request), multiParams(request), request.headers)
      case _ => newCommand.bindTo(params(request), multiParams(request), request.headers)
    }
    request.update(commandRequestKey[T], newCommand)
    newCommand
  }

}
