package org.scalatra
package swagger

import collection.mutable
import collection.JavaConverters._
import reflect.{ ManifestFactory, Reflector }
import util.RicherString._
import javax.servlet.{ Servlet, Filter }
import scala.util.parsing.combinator.RegexParsers
import scala.util.control.Exception.allCatch
import org.scalatra.swagger.DataType.{ ContainerDataType, ValueDataType }
import org.json4s.JsonFormat
import org.scalatra.util.NotNothing

trait SwaggerSupportBase {
  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[SwaggerEndpoint[_ <: SwaggerOperation]]

  /**
   * Extract an operation from a route
   */
  protected def extractOperation(route: Route, method: HttpMethod): SwaggerOperation
}
object SwaggerSupportSyntax {
  private[swagger] case class Entry[T <: SwaggerOperation](key: String, value: T)

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

      def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

      private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
        tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
      }

      private def token: Parser[Builder => Builder] = splat | prefixedOptional | optional | named | literal

      private def splat: Parser[Builder => Builder] = "*" ^^^ { builder => builder addSplat }

      private def prefixedOptional: Parser[Builder => Builder] =
        ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
          case p ~ "?:" ~ o ~ "?" => builder => builder addPrefixedOptional (o, p)
        }

      private def optional: Parser[Builder => Builder] =
        "?:" ~> """\w+""".r <~ "?" ^^ { str => builder => builder addOptional str }

      private def named: Parser[Builder => Builder] =
        ":" ~> """\w+""".r ^^ { str => builder => builder addNamed str }

      private def literal: Parser[Builder => Builder] =
        ("""[\.\+\(\)\$]""".r | ".".r) ^^ { str => builder => builder addLiteral str }
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

      def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

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

  private val SingleValued = Set(ParamType.Body, ParamType.Path)
  trait SwaggerParameterBuilder {
    private[this] var _dataType: DataType = _
    private[this] var _name: String = _
    private[this] var _description: Option[String] = None
    private[this] var _notes: Option[String] = None
    private[this] var _paramType: ParamType.ParamType = ParamType.Query
    private[this] var _allowableValues: AllowableValues = AllowableValues.AnyValue
    protected[this] var _required: Option[Boolean] = None
    //    private[this] var _allowMultiple: Boolean = false
    private[this] var _paramAccess: Option[String] = None

    def dataType: DataType = _dataType
    def dataType(dataType: DataType): this.type = { _dataType = dataType; this }
    def name(name: String): this.type = { _name = name; this }
    def description(description: String): this.type = { _description = description.blankOption; this }
    def description(description: Option[String]): this.type = { _description = description.flatMap(_.blankOption); this }

    def notes(notes: String): this.type = { _notes = notes.blankOption; this }
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

    def defaultValue: Option[String] = None

    def name: String = _name
    def description: Option[String] = _description
    def notes: Option[String] = _notes
    def paramType: ParamType.ParamType = _paramType
    def paramAccess = _paramAccess
    def allowableValues: AllowableValues = _allowableValues
    def isRequired: Boolean = paramType == ParamType.Path || _required.forall(identity)

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

    //    def allowsMultiple: Boolean = !SwaggerSupportSyntax.SingleValued.contains(paramType) && _allowMultiple

    def result =
      Parameter(name, dataType, description, notes, paramType, defaultValue, allowableValues, isRequired)
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
  }

  class ModelParameterBuilder(val initialDataType: DataType) extends SwaggerParameterBuilder {
    dataType(initialDataType)
  }

  abstract class SwaggerOperationBuilder[T <: SwaggerOperation] {

    private[this] var _summary: String = ""
    private[this] var _notes: String = ""
    private[this] var _deprecated: Boolean = false
    private[this] var _nickname: String = _
    private[this] var _parameters: List[Parameter] = Nil
    private[this] var _responseMessages: List[ResponseMessage[_]] = Nil
    private[this] var _produces: List[String] = Nil
    private[this] var _consumes: List[String] = Nil
    private[this] var _protocols: List[String] = Nil
    private[this] var _authorizations: List[String] = Nil
    private[this] var _position: Int = 0

    def resultClass: DataType

    def summary(content: String): this.type = {
      _summary = content
      this
    }
    def summary: String = _summary
    def notes(content: String): this.type = {
      _notes = content
      this
    }
    def notes: Option[String] = _notes.blankOption
    def deprecated(value: Boolean): this.type = {
      _deprecated = value
      this
    }
    def deprecated: Boolean = _deprecated
    def deprecate: this.type = { _deprecated = true; this }
    def nickname(value: String): this.type = { _nickname = value; this }
    def nickName(value: String): this.type = nickname(value)
    def nickname: Option[String] = _nickname.blankOption
    def parameters(params: Parameter*): this.type = { _parameters :::= params.toList; this }
    def parameter(param: Parameter): this.type = parameters(param)
    def parameters: List[Parameter] = _parameters
    def responseMessages: List[ResponseMessage[_]] = _responseMessages
    def responseMessages(errs: ResponseMessage[_]*): this.type = { _responseMessages :::= errs.toList; this }
    def responseMessage(err: ResponseMessage[_]): this.type = responseMessages(err)
    def produces(values: String*): this.type = { _produces :::= values.toList; this }
    def produces: List[String] = _produces
    def consumes: List[String] = _consumes
    def consumes(values: String*): this.type = { _consumes :::= values.toList; this }
    def protocols: List[String] = _protocols
    def protocols(values: String*): this.type = { _protocols :::= values.toList; this }
    def authorizations: List[String] = _authorizations
    def authorizations(values: String*): this.type = { _authorizations :::= values.toList; this }
    def position(value: Int): this.type = { _position = value; this }
    def position: Int = _position

    @deprecated("Swagger spec 1.2 defines errors as responseMessages", "2.2.2")
    def errors(errs: ResponseMessage[_]*): this.type = responseMessages(errs: _*)
    @deprecated("Swagger spec 1.2 defines error as responseMessage", "2.2.2")
    def error(err: ResponseMessage[_]): this.type = responseMessages(err)
    @deprecated("Swagger spec 1.2 defines errors as responseMessages", "2.2.2")
    def errorResponses: List[ResponseMessage[_]] = responseMessages

    def result: T
  }

  class OperationBuilder(val resultClass: DataType) extends SwaggerOperationBuilder[Operation] {
    def result: Operation = Operation(
      null,
      resultClass,
      summary,
      position,
      notes,
      deprecated,
      nickname,
      parameters,
      responseMessages,
      consumes,
      produces,
      protocols,
      authorizations)
  }
}
trait SwaggerSupportSyntax extends Initializable with CorsSupport { this: ScalatraBase with SwaggerSupportBase =>
  protected implicit def swagger: SwaggerEngine[_]

  @deprecated("This field is no longer used, due to changes in Swagger spec 1.2", "2.3.1")
  protected def applicationName: Option[String] = None
  protected def applicationDescription: String
  @deprecated("Swagger spec 1.2 renamed this to swaggerDefaultMessages, please use that one", "2.2.2")
  protected def swaggerDefaultErrors: List[ResponseMessage[_]] = swaggerDefaultMessages
  protected def swaggerDefaultMessages: List[ResponseMessage[_]] = Nil
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
  abstract override def initialize(config: ConfigT) {
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

  @deprecated("This implicit conversion will be removed in the future", "2.2")
  implicit protected def modelToSwagger(cls: Class[_]): (String, Model) = {
    val mod = Swagger.modelToSwagger(Reflector.scalaTypeOf(cls)).get // TODO: the use of .get is pretty dangerous, but it's deprecated
    mod.id -> mod
  }

  private[swagger] val _models: mutable.Map[String, Model] = mutable.Map.empty

  /**
   * Registers a model for swagger
   * @param model the model to add to the swagger definition
   */
  protected def registerModel(model: Model) {
    _models.getOrElseUpdate(model.id, model)
  }

  /**
   * Registers a model for swagger, this method reflects over the class and collects all
   * non-primitive classes and adds those to the swagger defintion
   * @tparam T the class of the model to register
   */
  protected def registerModel[T: Manifest: NotNothing]() {
    Swagger.collectModels[T](_models.values.toSet) map registerModel
  }

  @deprecated("Use `registerModel[T]` or `registerModel(model)` instead, this method will be removed in the future", "2.2")
  protected def models_=(m: Map[String, Model]) = _models ++= m

  /**
   * The currently registered model descriptions for swagger
   * @return a map of swagger models
   */
  def models = _models

  private[swagger] var _description: PartialFunction[String, String] = Map.empty
  protected def description(f: PartialFunction[String, String]) = _description = f orElse _description

  @deprecated("Use the `apiOperation.summary` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def summary(value: String) = swaggerMeta(Symbols.Summary, value)
  @deprecated("Use the `apiOperation.notes` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def notes(value: String) = swaggerMeta(Symbols.Notes, value)
  @deprecated("Use the variant where you use a type parameter, this method doesn't allow for reflection and requires you to manually ad the model", "2.2")
  protected def responseClass(value: String) = swaggerMeta(Symbols.ResponseClass, value)
  @deprecated("Use the `apiOperation.responseClass` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def responseClass[T](implicit mf: Manifest[T]) = {
    registerModel[T]()
    swaggerMeta(Symbols.ResponseClass, DataType[T])
  }
  @deprecated("Use the `apiOperation.nickname` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def nickname(value: String) = swaggerMeta(Symbols.Nickname, value)

  protected def endpoint(value: String) = swaggerMeta(Symbols.Endpoint, value)
  @deprecated("Use the `apiOperation.parameters` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def parameters(value: Parameter*) = swaggerMeta(Symbols.Parameters, value.toList)
  @deprecated("Use the `apiOperation.errors` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def errors(value: Error*) = swaggerMeta(Symbols.Errors, value.toList)

  import SwaggerSupportSyntax._
  protected def apiOperation[T: Manifest: NotNothing](nickname: String): SwaggerOperationBuilder[_ <: SwaggerOperation]
  implicit def parameterBuilder2parameter(pmb: SwaggerParameterBuilder): Parameter = pmb.result

  private[this] def swaggerParam[T: Manifest](
    name: String, liftCollection: Boolean = false, allowsCollection: Boolean = true, allowsOption: Boolean = true): ParameterBuilder[T] = {
    val st = Reflector.scalaTypeOf[T]
    if (st.isCollection && !allowsCollection) sys.error("Parameter [" + name + "] does not allow for a collection.")
    if (st.isOption && !allowsOption) sys.error("Parameter [" + name + "] does not allow optional values.")
    if (st.isCollection && st.typeArgs.isEmpty) sys.error("A collection needs to have a type for swagger parameter [" + name + "].")
    if (st.isOption && st.typeArgs.isEmpty) sys.error("An Option needs to have a type for swagger parameter [" + name + "].")
    Swagger.collectModels(st, models.values.toSet) map registerModel
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

  protected def operation(op: SwaggerOperation) = swaggerMeta(Symbols.Operation, op)

  protected def swaggerMeta(s: Symbol, v: Any): RouteTransformer = { (route: Route) ⇒
    route.copy(metadata = route.metadata + (s -> v))
  }
  implicit def dataType2string(dt: DataType) = dt.name

  protected def inferSwaggerEndpoint(route: Route): String = route match {
    case rev if rev.isReversible =>
      rev.routeMatchers collectFirst {
        case sin: SinatraRouteMatcher => new SinatraSwaggerGenerator(sin).toSwaggerPath
        case rails: RailsRouteMatcher => new RailsSwaggerGenerator(rails).toSwaggerPath
        case path: PathPatternRouteMatcher => path.toString
      } getOrElse ""
    case _ => ""
  }

  protected def swaggerEndpointEntries[T <: SwaggerOperation](extract: (Route, HttpMethod) => T) =
    for {
      (method, routes) ← routes.methodRoutes
      route ← routes if (route.metadata.keySet & Symbols.AllSymbols).nonEmpty
      endpoint = route.metadata.get(Symbols.Endpoint) map (_.asInstanceOf[String]) getOrElse inferSwaggerEndpoint(route)
      operation = extract(route, method)
    } yield Entry(endpoint, operation)
}

/**
 * Provides the necessary support for adding documentation to your routes.
 */
trait SwaggerSupport extends ScalatraBase with SwaggerSupportBase with SwaggerSupportSyntax {

  import SwaggerSupportSyntax._

  protected implicit def operationBuilder2operation[T](bldr: SwaggerOperationBuilder[Operation]): Operation = bldr.result
  protected def apiOperation[T: Manifest: NotNothing](nickname: String): OperationBuilder = {
    registerModel[T]()
    (new OperationBuilder(DataType[T])
      nickname nickname)
  }
  protected def apiOperation(nickname: String, model: Model): OperationBuilder = {
    registerModel(model)
    (new OperationBuilder(ValueDataType(model.id))
      nickname nickname)
  }

  /**
   * Builds the documentation for all the endpoints discovered in an API.
   */
  def endpoints(basePath: String): List[Endpoint] = {
    (swaggerEndpointEntries(extractOperation) groupBy (_.key)).toList map {
      case (name, entries) ⇒
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
      val errors = route.metadata.get(Symbols.Errors) map (_.asInstanceOf[List[ResponseMessage[_]]]) getOrElse Nil
      val responseClass = route.metadata.get(Symbols.ResponseClass) map (_.asInstanceOf[DataType]) getOrElse DataType.Void
      val summary = (route.metadata.get(Symbols.Summary) map (_.asInstanceOf[String])).orNull
      val notes = route.metadata.get(Symbols.Notes) map (_.asInstanceOf[String])
      val nick = route.metadata.get(Symbols.Nickname) map (_.asInstanceOf[String])
      val produces = route.metadata.get(Symbols.Produces) map (_.asInstanceOf[List[String]]) getOrElse Nil
      val consumes = route.metadata.get(Symbols.Consumes) map (_.asInstanceOf[List[String]]) getOrElse Nil
      Operation(
        method = method,
        responseClass = responseClass,
        summary = summary,
        position = 0,
        notes = notes,
        nickname = nick,
        parameters = theParams,
        responseMessages = (errors ::: swaggerDefaultMessages ::: swaggerDefaultErrors).distinct,
        produces = produces,
        consumes = consumes)
    }
  }

}
