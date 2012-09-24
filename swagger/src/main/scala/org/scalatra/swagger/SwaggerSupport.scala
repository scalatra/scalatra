package org.scalatra
package swagger

import Symbols._
import collection.JavaConverters._
import util.RicherString._
import javax.servlet.{Servlet, Filter, Registration}
import com.wordnik.swagger.core.ApiPropertiesReader

/**
 * Provides the necessary support for adding documentation to your routes.
 */
trait SwaggerSupport extends Initializable {
  self: ScalatraBase =>


  protected implicit def swagger: Swagger

  protected def applicationName: Option[String]= None
  protected def applicationDescription: String

  private[this] def throwAFit =
    throw new IllegalStateException("I can't work out which servlet registration this is.")

  private[this] def registerInSwagger(name: String, servPath: String) = {
    val thePath = {
      val p = if (servPath.endsWith("/*")) servPath.dropRight(2) else servPath
      if (p.startsWith("/")) p else "/"+p
    }

    swagger.register(name, thePath, applicationDescription, this)
  }

  /**
   * Initializes the kernel.  Used to provide context that is unavailable
   * when the instance is constructed, for example the servlet lifecycle.
   * Should set the `config` variable to the parameter.
   *
   * @param config the configuration.
   */
  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    this match {
      case _: Filter =>
        val registrations = servletContext.getFilterRegistrations.asScala.values
        val registration = registrations.find(_.getClassName == getClass.getName) getOrElse throwAFit
        registration.getServletNameMappings.asScala foreach { name =>
          Option(servletContext.getServletRegistration(name)) foreach { reg =>
            reg.getMappings.asScala foreach (registerInSwagger(applicationName getOrElse reg.getName, _))
          }
        }

      case _: Servlet =>
        val registration = ScalatraBase.getServletRegistration(this) getOrElse throwAFit
        registration.getMappings.asScala foreach (registerInSwagger(applicationName getOrElse registration.getName, _))

      case _ => throw new RuntimeException("The swagger support only works for servlets or filters at this time.")
    }

  }

  implicit protected def modelToSwagger(cls: Class[_]): (String, Model) = {
    val docObj = ApiPropertiesReader.read(cls)
    val name = docObj.getName
    val fields = for (field <- docObj.getFields.asScala.filter(d => d.paramType != null))
      yield (field.name -> ModelField(field.name, field.notes, DataType(field.paramType)))

    Model(name, name, fields.toMap)
  }

  private var _models: Map[String, Model] = Map.empty
  protected def models_=(m: Map[String, Model]) = _models = m
  def models = _models

  private var _description: PartialFunction[String, String] = Map.empty
  protected def description(f: PartialFunction[String, String]) = _description = _description orElse f

  private var _secured: PartialFunction[String, Boolean] = Map.empty
  protected def secured(f: PartialFunction[String, Boolean]) = _secured = _secured orElse f

  protected def summary(value: String) = swaggerMeta(Summary, value)
  protected def notes(value: String) = swaggerMeta(Notes, value)
  protected def responseClass(value: String) = swaggerMeta(ResponseClass, value)
  protected def responseClass[T](implicit mf: Manifest[T]) = swaggerMeta(ResponseClass, mf.erasure.getSimpleName)
  protected def nickname(value: String) = swaggerMeta(Nickname, value)
  protected def endpoint(value: String) = swaggerMeta(Symbols.Endpoint, value)
  protected def parameters(value: Parameter*) = swaggerMeta(Parameters, value.toList)
  protected def errors(value: Error*) = swaggerMeta(Errors, value.toList)

  protected def swaggerMeta(s: Symbol, v: Any): RouteTransformer = { route ⇒
    route.copy(metadata = route.metadata + (s -> v))
  }
  


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
    val theParams = route.metadata.get(Symbols.Parameters) map (_.asInstanceOf[List[Parameter]]) getOrElse Nil
    val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[Error]]) getOrElse Nil
    val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[String]) getOrElse DataType.Void.name
    val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
    val notes = route.metadata.get(Symbols.Notes) map (_.asInstanceOf[String])
    val nick = route.metadata.get(Symbols.Nickname) map (_.asInstanceOf[String])
    List(Operation(httpMethod = method,
      responseClass = responseClass,
      summary = summary,
      notes = notes,
      nickname = nick,
      parameters = theParams,
      errorResponses = errors))
  }

  implicit def dataType2string(dt: DataType.DataType) = dt.name

}
