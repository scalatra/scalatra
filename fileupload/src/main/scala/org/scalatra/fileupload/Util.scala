package org.scalatra.fileupload

import javax.servlet.http.Part

object Util {
  def partAttribute(part: Part,
                            headerName: String, attributeName: String,
                            defaultValue: String = null) = Option(part.getHeader(headerName)) match {
    case Some(value) => {
      value.split(";").find(_.trim().startsWith(attributeName)) match {
        case Some(attributeValue) => attributeValue.substring(attributeValue.indexOf('=') + 1).trim().replace("\"", "")
        case _                    => defaultValue
      }
    }

    case _ => defaultValue
  }
}
