package org.scalatra
package swagger

import Symbols._

/**
 * Provides the necessary support for adding documentation to your routes.
 */
trait SwaggerSupport {
  self: ScalatraApp =>

  private var _models: Map[String, Model] = Map.empty
  protected def models_=(m: Map[String, Model]) = _models = m

  private var _description: PartialFunction[String, String] = Map.empty
  protected def description(f: PartialFunction[String, String]) = _description = _description orElse f

  private var _secured: PartialFunction[String, Boolean] = Map.empty
  protected def secured(f: PartialFunction[String, Boolean]) = _secured = _secured orElse f

  protected def summary(value: String) = swaggerMeta(Summary, value)
  protected def notes(value: String) = swaggerMeta(Notes, value)
  protected def responseClass(value: String) = swaggerMeta(ResponseClass, value)
  protected def nickname(value: String) = swaggerMeta(Nickname, value)
  protected def endpoint(value: String) = swaggerMeta(Symbols.Endpoint, value)
  protected def parameters(value: Parameter*) = swaggerMeta(Parameters, value.toList)
  protected def parameters(value: List[Parameter]) = swaggerMeta(Parameters, value)
  protected def errors(value: Error*) = swaggerMeta(Errors, value.toList)
  protected def errors(value: List[Error]) = swaggerMeta(Errors, value)

  protected def swaggerMeta(s: Symbol, v: Any): RouteTransformer = { route ⇒
    route.copy(metadata = route.metadata + (s -> v))
  }
  
  def models = _models

  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String) = {
    case class Entry(key: String, value: List[Operation])
    val ops = (for {
      (method, routes) ← routes.methodRoutes
      route ← routes
    } yield {
      val endpoint = route.metadata.get(Symbols.Endpoint) map (_.asInstanceOf[String]) getOrElse ""
      Entry(endpoint, operations(route, method))
    }) filter (l ⇒ l.value.nonEmpty && l.value(0).nickname.isDefined) groupBy (_.key)
    (List.empty[Endpoint] /: ops) { (r, op) ⇒
      val name = op._1
      val sec = _secured.lift apply name getOrElse true
      val desc = _description.lift apply name getOrElse ""
      new Endpoint("%s/%s" format (basePath, name), desc, sec, op._2.toList flatMap (_.value)) :: r
    } sortWith { (a, b) ⇒ a.path < b.path }
  }

  /**
   * Returns a list of operations based on the given route. The default implementation returns a list with only 1
   * operation.
   */
  protected def operations(route: Route, method: HttpMethod): List[Operation] = {
    val params = route.metadata.get(Symbols.Parameters)
    val errors = route.metadata.get(Symbols.Errors)
    val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[String]) getOrElse DataType.Void.name
    val summary = route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String]) orNull
    val notes = route.metadata.get(Symbols.Notes) map (_.asInstanceOf[String])
    List(Operation(httpMethod = method,
      responseClass = responseClass,
      summary = summary,
      notes = notes,
      nickname = route.metadata.get(Symbols.Nickname) map (_.asInstanceOf[String]),
      parameters = params map (_.asInstanceOf[List[Parameter]]) getOrElse Nil,
      errorResponses = errors map (_.asInstanceOf[List[Error]]) getOrElse Nil))
  }

  implicit def dataType2string(dt: DataType.DataType) = dt.name

}
