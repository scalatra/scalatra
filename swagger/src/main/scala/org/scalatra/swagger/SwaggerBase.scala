package org.scalatra
package swagger

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.swagger.DataType.{ ContainerDataType, ValueDataType }
import org.slf4j.LoggerFactory
import scala.util.Try
import scala.collection.immutable.ListMap

/**
 * Trait that serves the resource and operation listings, as specified by the Swagger specification.
 */
trait SwaggerBase extends Initializable { self: ScalatraBase with CorsSupport =>

  private lazy val logger = LoggerFactory.getLogger(getClass)

  implicit protected def jsonFormats: Formats = DefaultFormats
  implicit protected def docToJson(doc: Api): JValue = Extraction.decompose(doc)

  implicit override def string2RouteMatcher(path: String) = new RailsRouteMatcher(path)

  implicit class JsonAssocNonEmpty(left: JObject) {
    def ~!(right: JObject): JObject = {
      right.obj.headOption match {
        case Some((_, JArray(arr))) if arr.isEmpty => left.obj
        case Some((_, JObject(fs))) if fs.isEmpty => left.obj
        case _ => JObject(left.obj ::: right.obj)
      }
    }
  }

  /**
   * The name of the route to use when getting the index listing for swagger
   * defaults to optional resources.:format or /
   * @return The name of the route
   */
  protected def indexRoute: String = "resources"

  /**
   * Whether to include the format parameter in the index listing for swagger
   * defaults to false, the format parameter will not be present but is still optional.
   * @return true if the format parameter should be included in the returned json
   */
  protected def includeFormatParameter: Boolean = false

  abstract override def initialize(config: ConfigT): Unit = {
    super.initialize(config)
    get("/swagger.json") {
      renderSwagger2(swagger.docs.toList.asInstanceOf[List[Api]])
    }
  }

  /**
   * Returns the Swagger instance responsible for generating the resource and operation listings.
   */
  protected implicit def swagger: SwaggerEngine

  //  private[this] def dontAddOnEmpty(key: String, value: List[String])(json: JValue) = {
  //    val v: JValue = if (value.nonEmpty) key -> value else JNothing
  //    json merge v
  //  }

  private[this] def generateDataType(dataType: DataType): List[JField] = {
    dataType match {
      case t: ValueDataType if t.qualifiedName.isDefined =>
        List(("$ref" -> s"#/definitions/${t.name}"))
      case t: ValueDataType =>
        List(("type" -> t.name), ("format" -> t.format))
      case t: ContainerDataType if t.name == "Map" =>
        List(("type" -> "object"), ("additionalProperties" -> generateDataType(t.typeArg.get)))
      case t: ContainerDataType =>
        List(("type" -> "array"), ("items" -> generateDataType(t.typeArg.get)))
    }
  }

  protected def bathPath: Option[String] = {
    val path = url("/", includeContextPath = swagger.baseUrlIncludeContextPath, includeServletPath = swagger.baseUrlIncludeServletPath)
    if (path.isEmpty) None else Some(path)
  }

  protected def renderSwagger2(docs: List[Api]): JValue = {
    ("swagger" -> "2.0") ~
      ("basePath" -> bathPath) ~
      ("host" -> { if (swagger.host.isEmpty) JNothing else JString(swagger.host) }) ~
      ("info" ->
        ("title" -> swagger.apiInfo.title) ~
        ("version" -> swagger.apiVersion) ~
        ("description" -> swagger.apiInfo.description) ~
        ("termsOfService" -> swagger.apiInfo.termsOfServiceUrl) ~
        ("contact" -> (
          ("name" -> swagger.apiInfo.contact.name) ~
          ("url" -> swagger.apiInfo.contact.url) ~
          ("email" -> swagger.apiInfo.contact.email))) ~
          ("license" -> (
            ("name" -> swagger.apiInfo.license.name) ~
            ("url" -> swagger.apiInfo.license.url)))) ~
            ("paths" ->
              ListMap(docs.filter(_.apis.nonEmpty).flatMap { doc =>
                doc.apis.collect {
                  case api: Endpoint =>
                    (api.path -> api.operations.sortBy(_.position).map { operation =>
                      val responses = operation.responseMessages.map { response =>
                        (response.code.toString ->
                          ("description", response.message) ~~
                          response.responseModel.map { model =>
                            List(JField("schema", JObject(JField("$ref", s"#/definitions/${model}"))))
                          }.getOrElse(Nil))
                      }.toMap
                      (operation.method.toString.toLowerCase -> (
                        ("operationId" -> operation.operationId) ~
                        ("summary" -> operation.summary) ~!
                        ("description" -> operation.description) ~!
                        ("schemes" -> operation.schemes) ~!
                        ("consumes" -> operation.consumes) ~!
                        ("produces" -> operation.produces) ~!
                        ("tags" -> operation.tags) ~
                        ("deprecated" -> operation.deprecated) ~
                        ("parameters" -> operation.parameters.sortBy(_.position).map { parameter =>
                          ("name" -> parameter.name) ~
                            ("description" -> parameter.description) ~
                            ("default" -> parameter.defaultValue) ~
                            ("minimum" -> parameter.minimumValue) ~
                            ("maximum" -> parameter.maximumValue) ~
                            ("required" -> parameter.required) ~
                            ("in" -> swagger2ParamTypeMapping(parameter.paramType.toString.toLowerCase)) ~~
                            (if (parameter.paramType.toString.toLowerCase == "body") {
                              List(JField("schema", generateDataType(parameter.`type`)))
                            } else {
                              generateDataType(parameter.`type`)
                            })
                        }) ~
                        ("responses" ->
                          (if (responses.nonEmpty && responses.keySet.exists(_.startsWith("2"))) responses else {
                            responses + ("200" ->
                              (if (operation.responseClass.name == "void") {
                                JObject(JField("description", "No response"))
                              } else {
                                JObject(JField("description", "OK"), JField("schema", generateDataType(operation.responseClass)))
                              }))
                          })) ~!
                          ("security" -> (operation.authorizations.flatMap { requirement =>
                            swagger.authorizations.find(_.keyName == requirement).map {
                              case a: OAuth => (requirement -> a.scopes)
                              case b: ApiKey => (requirement -> List.empty)
                              case _ => (requirement -> List.empty)
                            }
                          }))))
                    })
                }
              }: _*)) ~
              ("definitions" -> docs.flatMap { doc =>
                doc.models.map {
                  case (name, model) =>
                    (name ->
                      ("type" -> "object") ~
                      ("description" -> model.description) ~
                      ("discriminator" -> model.discriminator) ~
                      ("properties" -> model.properties.sortBy(_._2.position).map {
                        case (name, property) =>
                          (name ->
                            ("example" -> toTypedAst(property.example, property.`type`)) ~
                            ("minimum" -> property.minimumValue) ~
                            ("maximum" -> property.maximumValue) ~
                            ("description" -> property.description) ~~
                            generateDataType(property.`type`))
                      }.toMap) ~!
                      ("required" -> model.properties.collect {
                        case (name, property) if property.required => name
                      }))
                }
              }.toMap) ~
              ("securityDefinitions" -> (swagger.authorizations.flatMap { auth =>
                (auth match {
                  case a: OAuth => a.grantTypes.headOption.map {
                    case g: ImplicitGrant => (a.keyName -> JObject(
                      JField("type", "oauth2"),
                      JField("description", a.description),
                      JField("flow", "implicit"),
                      JField("authorizationUrl", g.loginEndpoint.url),
                      JField("scopes", a.scopes.map(scope => JField(scope, scope)))))
                    case g: AuthorizationCodeGrant => (a.keyName -> JObject(
                      JField("type", "oauth2"),
                      JField("description", a.description),
                      JField("flow", "accessCode"),
                      JField("authorizationUrl", g.tokenRequestEndpoint.url),
                      JField("tokenUrl", g.tokenEndpoint.url),
                      JField("scopes", a.scopes.map(scope => JField(scope, scope)))))
                    case g: ApplicationGrant => ("oauth2" -> JObject(
                      JField("type", "oauth2"),
                      JField("description", a.description),
                      JField("flow", "application"),
                      JField("tokenUrl", g.tokenEndpoint.url),
                      JField("scopes", a.scopes.map(scope => JField(scope, scope)))))
                  }
                  case a: ApiKey => Some((a.keyName -> JObject(
                    JField("type", "apiKey"),
                    JField("description", a.description),
                    JField("name", a.keyName),
                    JField("in", a.passAs))))
                })
              }).toMap)
  }

  private def toTypedAst(dataVal: Option[String], dataType: DataType): JValue = {
    if (dataVal.isEmpty) { JNothing }
    else if (!dataType.name.equalsIgnoreCase("string") && Try(dataVal.get.toDouble).isSuccess) { JDouble(dataVal.get.toDouble) }
    else { JString(dataVal.get) }
  }

  private def swagger2ParamTypeMapping(paramTypeName: String): String = {
    if (paramTypeName == "form") "formData" else paramTypeName
  }

  error {
    case t: Throwable =>
      logger.error("Error during rendering swagger.json", t)
      throw t
  }

}
