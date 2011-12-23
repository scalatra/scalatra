package org.scalatra.docs

import org.scalatra.{Get, HttpMethod}

case class Documentation(name: String, route: String, description: String, method: HttpMethod = Get, requiredParams: List[Param] = List(), optionalParams: List[Param] = List()) {

  implicit def list2HtmlList(list: List[Param]): ParamList = ParamList(list)

  def toHtml() = {
    """
    <h1>%s %s</h1>
    <h2>%s</h2>
    <p>%s</p>
    <h3>Required parameters</h3>
    %s
    <h3>Optional parameters</h3>
    %s
    """.format(method, route, name, description, requiredParams.toHtml, optionalParams.toHtml)
  }
  
}

case class Param(name: String, description: String) {
  def toHtml() = "%s - %s".format(name, description)          
}

case class ParamList(list: List[Param]){
    def toHtml = list.map( _.toHtml ).mkString("<ul><li>", "</li><li>", "</li></ul>")
}