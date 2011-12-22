package org.scalatra.docs

import org.scalatra.{Get, HttpMethod}

case class Documentation(name: String, route: String, method: HttpMethod = Get) {

  def toHtml() = {
    """
    <h1>%s %s</h1>
    <h2>%s</h2>
    """.format(method, route, name)
  }

}