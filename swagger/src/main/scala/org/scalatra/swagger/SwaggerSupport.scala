package org.scalatra
package swagger

import jakarta.servlet.{ Filter, Servlet }

import org.scalatra.swagger.DataType.{ ContainerDataType, ValueDataType }
import org.scalatra.swagger.reflect.Reflector
import org.scalatra.util.NotNothing
import org.scalatra.util.RicherString._

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.control.Exception.allCatch
import scala.util.parsing.combinator.RegexParsers

trait SwaggerSupportBase {
  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[Endpoint]

  /**
   * Extract an operation from a route
   */
  protected def extractOperation(route: Route, method: HttpMethod): Operation
}

object SwaggerSupportSyntax {
  private[swagger] case class Entry(key: String, value: Operation)

  class SinatraSwaggerGenerator(matcher: SinatraRouteMatcher) {
    def toSwaggerPath: String = BuilderGeneratorParser(matcher.toString)(Builder("")).get

    case class Builder(path: String) {

      def addLiteral(text: String): Builder = copy(path = path + text)

      def addSplat: Builder = throw new ScalatraException("Splats are not supported for swagger path inference")

      def addNamed(name: String): Builder =
        copy(path = path + "{" + name + "}")

      def addOptional(name: String): Builder =
        copy(path = path + "{" + name + "}")

      def addPrefixedOptional(name: String, prefix: String): Builder =
        copy(path = path + prefix + "{" + name + "}")

      // checks all splats are used, appends additional params as a query string
      def get: String = path
    }

    object BuilderGeneratorParser extends RegexParsers {

      def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern).get

      private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
        tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
      }

      private def token: Parser[Builder => Builder] = splat | prefixedOptional | optional | named | literal

      private def splat: Parser[Builder => Builder] = "*" ^^^ { builder => builder.addSplat }

      private def prefixedOptional: Parser[Builder => Builder] =
        ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
          case p ~ "?:" ~ o ~ "?" => builder => builder.addPrefixedOptional(o, p)
        }

      private def optional: Parser[Builder => Builder] =
        "?:" ~> """\w+""".r <~ "?" ^^ { str => builder => builder.addOptional(str) }

      private def named: Parser[Builder => Builder] =
        ":" ~> """\w+""".r ^^ { str => builder => builder.addNamed(str) }

      private def literal: Parser[Builder => Builder] =
        ("""[\.\+\(\)\$]""".r | ".".r) ^^ { str => builder => builder.addLiteral(str) }
    }
  }

  class RailsSwaggerGenerator(matcher: RailsRouteMatcher) {
    def toSwaggerPath: String = BuilderGeneratorParser(matcher.toString())(Builder("")).get

    case class Builder(path: String) {

      def addStatic(text: String): Builder = copy(path = path + text)

      def addParam(name: String): Builder =
        copy(path = path + "{" + name + "}")

      def optional(builder: Builder => Builder): Builder =
        try builder(this)
        catch { case e: Exception => this }

      // appends additional params as a query string
      def get: String = path
    }

    object BuilderGeneratorParser extends RegexParsers {

      def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern).get

      private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
        tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
      }

      //private def token = param | glob | optional | static
      private def token: Parser[Builder => Builder] = param | glob | optional | static

      private def param: Parser[Builder => Builder] =
        ":" ~> identifier ^^ { str => builder => builder addParam str }

      private def glob: Parser[Builder => Builder] =
        "*" ~> identifier ^^ { str => builder => builder addParam str }

      private def optional: Parser[Builder => Builder] =
        "(" ~> tokens <~ ")" ^^ { subBuilder => builder => builder optional subBuilder }

      private def static: Parser[Builder => Builder] =
        (escaped | char) ^^ { str => builder => builder addStatic str }

      private def identifier = """[a-zA-Z_]\w*""".r

      private def escaped = literal("\\") ~> (char | paren)

      private def char = metachar | stdchar

      private def metachar = """[.^$|?+*{}\\\[\]-]""".r

      private def stdchar = """[^()]""".r

      private def paren = ("(" | ")")
    }
  }

  trait SwaggerParameterBuilder {
    private[this] var _dataType: DataType = _
    private[this] var _name: String = _
    private[this] var _description: Option[String] = None
    private[this] var _paramType: ParamType.ParamType = ParamType.Query
    private[this] var _allowableValues: AllowableValues = AllowableValues.AnyValue
    protected[this] var _required: Option[Boolean] = None
    protected[this] var _hidden: Option[Boolean] = None
    private[this] var _paramAccess: Option[String] = None
    private[this] var _position: Option[Int] = None

    def dataType: DataType = _dataType
    def dataType(dataType: DataType): this.type = { _dataType = dataType; this }
    def name(name: String): this.type = { _name = name; this }
    def description(description: String): this.type = { _description = description.blankOption; this }
    def description(description: Option[String]): this.type = { _description = description.flatMap(_.blankOption); this }
    def position(position: Int): this.type = { _position = Some(position); this }

    def paramType(name: ParamType.ParamType): this.type = { _paramType = name; this }

    def fromBody: this.type = paramType(ParamType.Body)
    def fromPath: this.type = paramType(ParamType.Path)
    def fromQuery: this.type = paramType(ParamType.Query)
    def fromHeader: this.type = paramType(ParamType.Header)
    def fromForm: this.type = paramType(ParamType.Form)

    def allowableValues[V](values: V*): this.type = {
      _allowableValues = if (values.isEmpty) AllowableValues.empty else AllowableValues(values: _*)
      this
    }
    def allowableValues[V](values: List[V]): this.type = {
      _allowableValues = if (values.isEmpty) AllowableValues.empty else AllowableValues(values)
      this
    }

    def accessibleBy(value: String) = { _paramAccess = value.blankOption; this }
    def allowableValues(values: Range): this.type = { _allowableValues = AllowableValues(values); this }
    def required: this.type = { _required = Some(true); this }
    def optional: this.type = { _required = Some(false); this }
    def hidden: this.type = { _hidden = Some(true); this }

    def defaultValue: Option[String] = None

    def minimumValue: Option[Double] = None
    def maximumValue: Option[Double] = None
    def example: Option[String] = None
    def position: Option[Int] = _position

    def name: String = _name
    def description: Option[String] = _description
    def paramType: ParamType.ParamType = _paramType
    def paramAccess = _paramAccess
    def allowableValues: AllowableValues = _allowableValues
    def isRequired: Boolean = paramType == ParamType.Path || _required.forall(identity)
    def isHidden: Boolean = _hidden.getOrElse(false)

    def multiValued: this.type = {
      dataType match {
        case dt: ValueDataType => dataType(ContainerDataType("List", Some(dt), uniqueItems = false))
        case _ => this
      }
    }
    def singleValued: this.type = {
      dataType match {
        case ContainerDataType(_, Some(dataType), _) => this.dataType(dataType)
        case _ => this
      }
    }

    def result =
      Parameter(name, dataType, description, paramType, defaultValue, allowableValues, isRequired, position.getOrElse(0), example, minimumValue, maximumValue, isHidden)
  }

  class ParameterBuilder[T: Manifest](initialDataType: DataType) extends SwaggerParameterBuilder {
    dataType(initialDataType)
    private[this] var _defaultValue: Option[String] = None
    override def defaultValue = _defaultValue
    def defaultValue(value: T): this.type = {
      if (_required.isEmpty) optional
      _defaultValue = allCatch.withApply(_ => None) { value.toString.blankOption }
      this
    }

    private[this] var _minimumValue: Option[Double] = None
    override def minimumValue = _minimumValue
    def minimumValue(value: Double): this.type = {
      _minimumValue = Option(value)
      this
    }

    private[this] var _maximumValue: Option[Double] = None
    override def maximumValue = _maximumValue
    def maximumValue(value: Double): this.type = {
      _maximumValue = Option(value)
      this
    }

    private[this] var _example: Option[String] = None
    override def example = _example
    def example(value: String): this.type = {
      _example = Option(value)
      this
    }
  }

  class ModelParameterBuilder(val initialDataType: DataType) extends SwaggerParameterBuilder {
    dataType(initialDataType)
  }

  class OperationBuilder(val resultClass: DataType) {
    private[this] var _summary: String = ""
    private[this] var _description: String = ""
    private[this] var _deprecated: Boolean = false
    private[this] var _operationId: String = _
    private[this] var _parameters: List[Parameter] = Nil
    private[this] var _responseMessages: List[ResponseMessage] = Nil
    private[this] var _produces: List[String] = Nil
    private[this] var _consumes: List[String] = Nil
    private[this] var _schemes: List[String] = Nil
    private[this] var _authorizations: List[String] = Nil
    private[this] var _tags: List[String] = Nil
    private[this] var _position: Int = 0

    def summary(content: String): this.type = { _summary = content; this }
    def summary: String = _summary
    def description(content: String): this.type = { _description = content; this }
    def description: Option[String] = _description.blankOption
    def deprecated(value: Boolean): this.type = { _deprecated = value; this }
    def deprecated: Boolean = _deprecated
    def deprecate: this.type = { _deprecated = true; this }
    def operationId(value: String): this.type = { _operationId = value; this }
    def operationId: String = _operationId
    def parameters(params: Parameter*): this.type = { _parameters :::= params.toList; this }
    def parameter(param: Parameter): this.type = parameters(param)
    def parameters: List[Parameter] = _parameters
    def responseMessages: List[ResponseMessage] = _responseMessages
    def responseMessages(errs: ResponseMessage*): this.type = { _responseMessages :::= errs.toList; this }
    def responseMessage(err: ResponseMessage): this.type = responseMessages(err)
    def produces(values: String*): this.type = { _produces :::= values.toList; this }
    def produces: List[String] = _produces
    def consumes: List[String] = _consumes
    def consumes(values: String*): this.type = { _consumes :::= values.toList; this }
    def schemes: List[String] = _schemes
    def schemes(values: String*): this.type = { _schemes :::= values.toList; this }
    def authorizations: List[String] = _authorizations
    def authorizations(values: String*): this.type = { _authorizations :::= values.toList; this }
    def tags: List[String] = _tags
    def tags(values: String*): this.type = { _tags :::= values.toList; this }
    def position(value: Int): this.type = { _position = value; this }
    def position: Int = _position

    def result: Operation = Operation(
      null,
      operationId,
      resultClass,
      summary,
      position,
      description,
      deprecated,
      parameters,
      responseMessages,
      consumes,
      produces,
      schemes,
      authorizations,
      tags)
  }
}

trait SwaggerSupportSyntax extends Initializable with CorsSupport {
  this: ScalatraBase with SwaggerSupportBase =>
  protected implicit def swagger: SwaggerEngine

  protected def applicationDescription: String

  protected def swaggerDefaultMessages: List[ResponseMessage] = Nil

  protected def swaggerProduces: List[String] = List("application/json")

  protected def swaggerConsumes: List[String] = List("application/json")

  protected def swaggerProtocols: List[String] = List("http")

  protected def swaggerAuthorizations: List[String] = Nil

  private[this] def throwAFit =
    throw new IllegalStateException("I can't work out which servlet registration this is.")

  private[this] def registerInSwagger(servPath: String) = {
    val resourcePath = {
      val p = if (servPath.endsWith("/*")) servPath.dropRight(2) else servPath
      if (p.startsWith("/")) p else "/" + p
    }
    val listingPath = resourcePath.drop(1) // drop the leading slash

    swagger.register(listingPath, resourcePath, applicationDescription.blankOption, this, swaggerConsumes, swaggerProduces, swaggerProtocols, swaggerAuthorizations)
  }

  /**
   * Initializes the kernel.  Used to provide context that is unavailable
   * when the instance is constructed, for example the servlet lifecycle.
   * Should set the `config` variable to the parameter.
   *
   * @param config the configuration.
   */
  abstract override def initialize(config: ConfigT): Unit = {
    super.initialize(config)
    try {
      this match {
        case _: Filter =>
          val registrations = servletContext.getFilterRegistrations.asScala.values
          val registration = registrations.find(_.getClassName == getClass.getName) getOrElse throwAFit
          registration.getServletNameMappings.asScala foreach { name =>
            Option(servletContext.getServletRegistration(name)) foreach { reg =>
              reg.getMappings.asScala foreach registerInSwagger
            }
          }

        case _: Servlet =>
          val registration = ScalatraBase.getServletRegistration(this) getOrElse throwAFit
          //          println("Registering for mappings: " + registration.getMappings().asScala.mkString("[", ", ", "]"))
          registration.getMappings.asScala foreach registerInSwagger

        case _ => throw new RuntimeException("The swagger support only works for servlets or filters at this time.")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }

  }

  private[swagger] val _models: mutable.Map[String, Model] = mutable.Map.empty

  /**
   * Registers a model for swagger
   *
   * @param model the model to add to the swagger definition
   */
  protected def registerModel(model: Model): Unit = {
    _models.getOrElseUpdate(model.id, model)
  }

  /**
   * Registers a model for swagger, this method reflects over the class and collects all
   * non-primitive classes and adds those to the swagger defintion
   *
   * @tparam T the class of the model to register
   */
  protected def registerModel[T: Manifest: NotNothing](): Unit = {
    Swagger.collectModels[T](_models.values.toSet) foreach registerModel
  }

  /**
   * The currently registered model descriptions for swagger
   *
   * @return a map of swagger models
   */
  def models = _models

  private[swagger] var _description: PartialFunction[String, String] = Map.empty

  protected def description(f: PartialFunction[String, String]) = _description = f orElse _description

  protected def endpoint(value: String) = swaggerMeta(Symbols.Endpoint, value)

  import org.scalatra.swagger.SwaggerSupportSyntax._

  protected def apiOperation[T: Manifest: NotNothing](nickname: String): OperationBuilder

  implicit def parameterBuilder2parameter(pmb: SwaggerParameterBuilder): Parameter = pmb.result

  private[this] def swaggerParam[T: Manifest](
    name: String, liftCollection: Boolean = false, allowsCollection: Boolean = true, allowsOption: Boolean = true): ParameterBuilder[T] = {
    val st = Reflector.scalaTypeOf[T]
    if (st.isCollection && !allowsCollection) sys.error("Parameter [" + name + "] does not allow for a collection.")
    if (st.isOption && !allowsOption) sys.error("Parameter [" + name + "] does not allow optional values.")
    if (st.isCollection && st.typeArgs.isEmpty) sys.error("A collection needs to have a type for swagger parameter [" + name + "].")
    if (st.isOption && st.typeArgs.isEmpty) sys.error("An Option needs to have a type for swagger parameter [" + name + "].")
    Swagger.collectModels(st, models.values.toSet) foreach registerModel
    val dt =
      if (liftCollection && (st.isCollection || st.isOption)) DataType.fromScalaType(st.typeArgs.head)
      else DataType[T]

    val b = new ParameterBuilder[T](dt).name(name)
    if (st.isOption) b.optional
    if (st.isCollection) b.multiValued

    Swagger.modelToSwagger[T] foreach { m => b.description(m.description) }
    b
  }

  private[this] def swaggerParam(name: String, model: Model): ModelParameterBuilder = {
    registerModel(model)
    new ModelParameterBuilder(DataType(model.id)).description(model.description).name(name)
  }

  protected def bodyParam[T: Manifest: NotNothing]: ParameterBuilder[T] = bodyParam[T]("body")

  protected def bodyParam(model: Model): ModelParameterBuilder = bodyParam("body", model)

  protected def bodyParam[T: Manifest: NotNothing](name: String): ParameterBuilder[T] = swaggerParam[T](name).fromBody

  protected def bodyParam(name: String, model: Model): ModelParameterBuilder = swaggerParam(name, model).fromBody

  protected def queryParam[T: Manifest: NotNothing](name: String): ParameterBuilder[T] = swaggerParam[T](name, liftCollection = true)

  protected def queryParam(name: String, model: Model): ModelParameterBuilder = swaggerParam(name, model)

  protected def formParam[T: Manifest: NotNothing](name: String): ParameterBuilder[T] = swaggerParam[T](name, liftCollection = true).fromForm

  protected def formParam(name: String, model: Model): ModelParameterBuilder = swaggerParam(name, model).fromForm

  protected def headerParam[T: Manifest: NotNothing](name: String): ParameterBuilder[T] =
    swaggerParam[T](name, allowsCollection = false).fromHeader

  protected def headerParam(name: String, model: Model): ModelParameterBuilder = swaggerParam(name, model).fromHeader

  protected def pathParam[T: Manifest: NotNothing](name: String): ParameterBuilder[T] =
    swaggerParam[T](name, allowsCollection = false, allowsOption = false).fromPath

  protected def pathParam(name: String, model: Model): ModelParameterBuilder = swaggerParam(name, model).fromPath

  protected def operation(op: Operation) = swaggerMeta(Symbols.Operation, op)

  protected def swaggerMeta(s: Symbol, v: Any): RouteTransformer = { (route: Route) =>
    route.copy(metadata = route.metadata + (s -> v))
  }

  implicit def dataType2string(dt: DataType): String = dt.name

  protected def inferSwaggerEndpoint(route: Route): String = route match {
    case rev if rev.isReversible =>
      rev.routeMatchers collectFirst {
        case sin: SinatraRouteMatcher => new SinatraSwaggerGenerator(sin).toSwaggerPath
        case rails: RailsRouteMatcher => new RailsSwaggerGenerator(rails).toSwaggerPath
        case path: PathPatternRouteMatcher => path.toString
      } getOrElse ""
    case _ => ""
  }

  protected def swaggerEndpointEntries(extract: (Route, HttpMethod) => Operation) =
    for {
      (method, routes) <- routes.methodRoutes
      route <- routes if (route.metadata.keySet & Symbols.AllSymbols).nonEmpty
      endpoint = route.metadata.get(Symbols.Endpoint) map (_.asInstanceOf[String]) getOrElse inferSwaggerEndpoint(route)
      operation = extract(route, method)
    } yield Entry(endpoint, operation)

  implicit class ResponseMessageWithModel(message: ResponseMessage) {
    def model[T: Manifest: NotNothing] = {
      swaggerParam[T]("response")
      message.copy(responseModel = Some(manifest[T].runtimeClass.getSimpleName))
    }
  }

  protected def swaggerTag: Option[String] = None

}

/**
 * Provides the necessary support for adding documentation to your routes.
 */
trait SwaggerSupport extends ScalatraBase with SwaggerSupportBase with SwaggerSupportSyntax {

  import org.scalatra.swagger.SwaggerSupportSyntax._

  protected implicit def operationBuilder2operation[T](bldr: OperationBuilder): Operation = bldr.result
  protected def apiOperation[T: Manifest: NotNothing](operationId: String): OperationBuilder = {
    registerModel[T]()
    makeOperationBuilder(operationId, DataType[T])
  }
  protected def apiOperation(operationId: String, model: Model): OperationBuilder = {
    registerModel(model)
    makeOperationBuilder(operationId, ValueDataType(model.id))
  }

  private def makeOperationBuilder(operationId: String, dataType: DataType): OperationBuilder = {
    val builder = new OperationBuilder(dataType).operationId(operationId)
    swaggerTag.foreach(builder.tags(_))
    builder
  }

  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[Endpoint] = {
    (swaggerEndpointEntries(extractOperation) groupBy (_.key)).toList map {
      case (name, entries) =>
        val desc = _description lift name getOrElse ""
        val pth = if (basePath endsWith "/") basePath else basePath + "/"
        val nm = if (name startsWith "/") name.substring(1) else name
        new Endpoint(pth + nm, desc.blankOption, entries.toList map (_.value))
    } sortBy (_.path)

  }

  /**
   * Returns a list of operations based on the given route. The default implementation returns a list with only 1
   * operation.
   */
  protected def extractOperation(route: Route, method: HttpMethod): Operation = {
    val op = route.metadata.get(Symbols.Operation) map (_.asInstanceOf[Operation])
    op map (_.copy(method = method)) getOrElse {
      val theParams = route.metadata.get(Symbols.Parameters) map (_.asInstanceOf[List[Parameter]]) getOrElse Nil
      val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[ResponseMessage]]) getOrElse Nil
      val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[DataType]) getOrElse DataType.Void
      val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
      val description = route.metadata.get(Symbols.Description) map (_.asInstanceOf[String])
      val operationId = route.metadata.get(Symbols.OperationId) map (_.asInstanceOf[String]) getOrElse ""
      val produces = route.metadata.get(Symbols.Produces) map (_.asInstanceOf[List[String]]) getOrElse Nil
      val consumes = route.metadata.get(Symbols.Consumes) map (_.asInstanceOf[List[String]]) getOrElse Nil
      Operation(
        method = method,
        operationId = operationId,
        responseClass = responseClass,
        summary = summary,
        position = 0,
        description = description,
        parameters = theParams,
        responseMessages = (errors ::: swaggerDefaultMessages).distinct,
        produces = produces,
        consumes = consumes)
    }
  }

}
