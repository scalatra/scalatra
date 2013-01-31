package org.scalatra
package swagger

import reflect.{ClassDescriptor, Reflector}
import Symbols._
import collection.JavaConverters._
import util.RicherString._
import javax.servlet.{ Servlet, Filter, Registration }
import com.wordnik.swagger.core.ApiPropertiesReader
import grizzled.slf4j.Logger

trait SwaggerSupportBase {
  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[SwaggerEndpoint[_ <: SwaggerOperation]]

  /**
   * Returns a list of operations based on the given route. The default implementation returns a list with only 1
   * operation.
   */
  protected def operations(route: Route, method: HttpMethod): List[SwaggerOperation]
}

trait SwaggerSupportSyntax extends Initializable with CorsSupport { this: ScalatraSyntax with SwaggerSupportBase =>
  protected implicit def swagger: SwaggerEngine[_]

  protected def applicationName: Option[String] = None
  protected def applicationDescription: String
  protected def swaggerDefaultErrors: List[Error] = Nil

  private[this] def throwAFit =
    throw new IllegalStateException("I can't work out which servlet registration this is.")

  private[this] def registerInSwagger(name: String, servPath: String) = {
    val thePath = {
      val p = if (servPath.endsWith("/*")) servPath.dropRight(2) else servPath
      if (p.startsWith("/")) p else "/" + p
    }
    val listingPath = {
      val inferred = inferListingPath()
      inferred map { lp =>
        val p = if (lp.endsWith("/*")) lp.dropRight(2) else lp
        val sp = applicationName getOrElse  (if (servPath.endsWith("/*")) servPath.dropRight(2) else servPath)
        val ssp = if (sp.startsWith("/")) sp else "/" + sp
        val lpp = if (p.startsWith("/")) p else "/" + p 
        if (lpp == "/") ssp else lpp + ssp
      }
    }

//    println("The listing path: %s" format listingPath)

    swagger.register(name, thePath, applicationDescription, this, listingPath)
  }

  private[this] def inferListingPath() = {
    val registrations = servletContext.getServletRegistrations.values().asScala.toList
    (registrations find { reg =>
      val klass = Class.forName(reg.getClassName)
      classOf[SwaggerBaseBase].isAssignableFrom(klass)
    } flatMap (_.getMappings.asScala.headOption))

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
    try {
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
//          println("Registering for mappings: " + registration.getMappings().asScala.mkString("[", ", ", "]"))
          registration.getMappings.asScala foreach (registerInSwagger(applicationName getOrElse registration.getName, _))

        case _ => throw new RuntimeException("The swagger support only works for servlets or filters at this time.")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }

  }

  @deprecated("This implicit conversion will be removed in the future", "2.2.0")
  implicit protected def modelToSwagger(cls: Class[_]): (String, Model) = {
    val docObj = ApiPropertiesReader.read(cls)
    val name = docObj.getName
    val fields = for (field <- docObj.getFields.asScala.filter(d => d.paramType != null))
      yield (field.name -> ModelField(field.name, field.notes, DataType(field.paramType)))

    Model(name, name, fields.toMap)
  }

  private[swagger] var _models: Map[String, Model] = Map.empty

  protected def registerModel(model: Model) = {
    models += model.id -> model
  }
  protected def registerModel[T:Manifest]() {
    models ++= Swagger.collectModels[T].map(m => m.id -> m)
  }

  @deprecated("Use `registerModel[T] or registerModel(model) instead, this method will be removed in the future", "2.2.0")
  protected def models_=(m: Map[String, Model]) = _models ++= m
  def models = _models

  private[swagger] var _description: PartialFunction[String, String] = Map.empty
  protected def description(f: PartialFunction[String, String]) = _description = _description orElse f


  protected def summary(value: String) = swaggerMeta(Summary, value)
  protected def notes(value: String) = swaggerMeta(Notes, value)
  @deprecated("Use the variant where you use a type parameter, this method doesn't allow for reflection and requires you to manually ad the model", "2.2.0")
  protected def responseClass(value: String) = swaggerMeta(ResponseClass, value)
  protected def responseClass[T](implicit mf: Manifest[T]) = {
    registerModel[T]
    swaggerMeta(ResponseClass, DataType[T].name)
  }
  protected def nickname(value: String) = swaggerMeta(Nickname, value)
  protected def endpoint(value: String) = swaggerMeta(Symbols.Endpoint, value)
  protected def parameters(value: Parameter*) = swaggerMeta(Parameters, value.toList)
  protected def errors(value: Error*) = swaggerMeta(Errors, value.toList)
  

  protected def swaggerMeta(s: Symbol, v: Any): RouteTransformer = { route ⇒
    route.copy(metadata = route.metadata + (s -> v))
  }
  implicit def dataType2string(dt: DataType.DataType) = dt.name

}

/**
 * Provides the necessary support for adding documentation to your routes.
 */
trait SwaggerSupport extends ScalatraSyntax with SwaggerSupportBase with SwaggerSupportSyntax {
  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[Endpoint] = {
    case class Entry(key: String, value: List[Operation])
    val ops = (for {
      (method, routes) ← routes.methodRoutes
      route ← routes
      endpoint = route.metadata.get(Symbols.Endpoint) map (_.asInstanceOf[String]) getOrElse ""
      operation = operations(route, method)
      if (operation.nonEmpty && operation.head.nickname.isDefined)
    } yield Entry(endpoint, operation))

    (ops groupBy (_.key)).toList map { case (name, entries) ⇒
      val sec = false //_secured.lift apply name getOrElse true
      val desc = _description lift name getOrElse ""
      val pth = if (basePath endsWith "/") basePath else basePath + "/"
      new Endpoint(pth + name, desc, sec, (entries.toList flatMap (_.value)) )
    } sortBy (_.path)

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
      errorResponses = errors ::: swaggerDefaultErrors))
  }


}
