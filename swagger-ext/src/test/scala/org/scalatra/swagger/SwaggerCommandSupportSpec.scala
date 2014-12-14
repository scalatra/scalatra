package org.scalatra
package swagger

import org.json4s._
import org.scalatra.commands._
import org.scalatra.test.specs2.MutableScalatraSpec

object SwaggerCommandSupportSpec {
  implicit val stringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)
  class SimpleCommand extends ParamsOnlyCommand {
    val name: Field[String] = asString("name").notBlank.position(1)
    val age: Field[Int] = bind[Int]("age").optional(0)
  }

  class FullCommand extends JsonCommand {
    protected implicit val jsonFormats: Formats = DefaultFormats

    import org.scalatra.commands.ValueSource._

    val name: Field[String] = asString("name").notBlank.position(1)
    val age: Field[Int] = bind[Int]("age").withDefaultValue(0)
    val token: Field[String] = (
      asString("API-TOKEN").notBlank
      sourcedFrom Header
      description "The API token for this request"
      notes "Invalid data kills kittens"
      allowableValues "123")
    val skip: Field[Int] = asInt("skip").withDefaultValue(0).sourcedFrom(Query).description("The offset for this collection index")
    val limit: Field[Int] = asType[Int]("limit").sourcedFrom(Query).withDefaultValue(20).description("the max number of items to return")
  }

  class CommandSupportServlet()(protected implicit val swagger: Swagger) extends ScalatraServlet with ParamsOnlyCommandSupport with SwaggerSupport with SwaggerCommandSupport {

    val allOperation = apiOperation[Unit]("all").parametersFromCommand[SimpleCommand]
    val newOperation = apiOperation[Unit]("new").parametersFromCommand(new SimpleCommand)
    get("/all", endpoint("all"), operation(allOperation)) { "OK" }
    get("/new", endpoint("new"), operation(newOperation)) { "OK" }

    protected def applicationDescription: String = "The command support servlet"

    override protected def applicationName: Option[String] = Some("support")

  }
}
class SwaggerCommandSupportSpec extends MutableScalatraSpec {

  import org.scalatra.swagger.SwaggerCommandSupportSpec._
  implicit val swagger = new Swagger("1.2", "1.0.0", SwaggerAuthSpec.apiInfo)
  addServlet(new CommandSupportServlet, "/")

  // Parameter(limit,ValueDataType(integer,Some(int32),None),Some(the max number of items to return),None,query,Some(Some(20)),AnyValue,false,None,0))
  // Parameter(limit,ValueDataType(integer,Some(int32),None),Some(the max number of items to return),None,query,Some(20),AnyValue,false,None,0)
  "SwaggerCommandSupport" should {
    "generate a model and parameters for a simple command" in {
      val (parameters, model) = SwaggerCommandSupport.parametersFromCommand(new SimpleCommand)
      parameters must_== List(Parameter("body", DataType("SimpleCommand"), None, paramType = ParamType.Body))
      model must beSome[Model]
      model.get.id must_== "SimpleCommand"
      model.get.description must beEmpty
      model.get.properties must containTheSameElementsAs(List("age" -> ModelProperty(DataType.Int, required = false), "name" -> ModelProperty(DataType.String, 1, required = true)))
    }

    "generate a model and parameters for a full command" in {
      val parameterList = List(
        Parameter("body", DataType("FullCommand"), None, paramType = ParamType.Body),
        Parameter("limit", DataType.Int, Some("the max number of items to return"), paramType = ParamType.Query, defaultValue = Some("20"), required = false),
        Parameter("skip", DataType.Int, Some("The offset for this collection index"), paramType = ParamType.Query, defaultValue = Some("0"), required = false),
        Parameter("API-TOKEN", DataType.String, Some("The API token for this request"), notes = Some("Invalid data kills kittens"), paramType = ParamType.Header, allowableValues = AllowableValues("123"))
      )
      val (parameters, model) = SwaggerCommandSupport.parametersFromCommand(new FullCommand)
      parameters.size must_== 3 // parameterList.size // disabled for swagger codegen for now
      parameters must contain(parameterList(0))
      parameters must contain(parameterList(1))
      parameters must contain(parameterList(2))
      // Disabled headers for now, until swagger codegen mangles header names
      //      parameters must contain(parameterList(3))
      model must beSome[Model]
      model.get.id must_== "FullCommand"
      model.get.description must beEmpty
      model.get.properties must containTheSameElementsAs(List("age" -> ModelProperty(DataType.Int, required = false), "name" -> ModelProperty(DataType.String, 1, required = true)))
    }
  }

}