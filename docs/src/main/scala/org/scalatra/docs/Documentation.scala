package org.scalatra.docs

import org.scalatra._

case class Documentation(name: Option[String], route: String, description: Option[String], method: HttpMethod = Get, requiredParams: List[Param] = List(), optionalParams: List[Param] = List(), document: Boolean = true) {

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
    """.format(method, route, name getOrElse "", description getOrElse "", requiredParams.toHtml, optionalParams.toHtml)
  }
  
}

object Documentation {

  def apply(route: Route, method: HttpMethod): Documentation = {
    val name = route.metadata.get(docsNameSymbol) filter (_.isInstanceOf[String]) map (_.asInstanceOf[String])
    val description = route.metadata.get(docsDescriptionSymbol) filter (_.isInstanceOf[String]) map (_.asInstanceOf[String])
    val optionalParams: List[Param] = route.metadata.get(docsOptionalParams) map (_.asInstanceOf[List[Param]]) getOrElse List()
    val requiredParams: List[Param] = route.metadata.get(docsRequiredParams) map (_.asInstanceOf[List[Param]]) getOrElse List()
    val requiredParamNames = requiredParams map ( _.name )
    val document = route.metadata.get(docsDocumentSymbol) filter (_.isInstanceOf[Boolean]) map (_.asInstanceOf[Boolean]) getOrElse true
    val paths = route.routeMatchers mkString ", "
    val kernelParams = route.routeMatchers flatMap { routeMatcher =>
      SinatraPathPatternParser(routeMatcher.toString).captureGroupNames } filterNot (requiredParamNames contains _)  map (Param(_, None)) toList

    Documentation(name, paths, description, method, requiredParams ++ kernelParams, optionalParams, document)
  }

}

case class Param(name: String, description: Option[String]) {
  def toHtml() = "%s - %s".format(name, description getOrElse "")
}

case class ParamList(list: List[Param]){
    def toHtml = list.map( _.toHtml ).mkString("<ul><li>", "</li><li>", "</li></ul>")
}