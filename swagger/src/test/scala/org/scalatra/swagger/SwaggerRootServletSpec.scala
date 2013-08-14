package org.scalatra
package swagger

import test.specs2.MutableScalatraSpec
import org.json4s._
import JsonDSL._
import native.JsonMethods
import java.net.ServerSocket
import io.Source
import org.json4s.native.JsonParser
import org.specs2.matcher.MatchResult

class SwaggerRootServletSpec extends MutableScalatraSpec {
  "Swagger integration for resources in a sub path should"    ^
    "list resources"                       ! listResources    ^
    "list operations"                      ! listOperations   ^
  end

  val swagger = new Swagger("1.1", "1")
  val testServlet = new SwaggerTestServlet(swagger)

  addServlet(testServlet, "/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/api-docs/*")
  implicit val formats = DefaultFormats

  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  override lazy val port: Int = { val s = new ServerSocket(0); try { s.getLocalPort } finally { s.close() } }//58468

  val listResourceJValue = readJson("subresources.json") merge (("basePath" -> ("http://localhost:" + port)):JValue)
//  println(JsonMethods.pretty(JsonMethods.render(listResourceJValue)))
  val listOperationsJValue = readJson("rootpet.json") merge (("basePath" -> ("http://localhost:" + port)):JValue)
//  println(JsonMethods.pretty(JsonMethods.render(listOperationsJValue)))

  private def readJson(file: String) = {
    val f = if ( file startsWith "/" ) file else "/"+file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }


  def listResources = get("/api-docs/api-docs.json") {
    JsonParser.parseOpt(body) must beSome(listResourceJValue)
  }

  val operations = "allPets" :: "updatePet" :: "addPet" :: "findByTags" :: "findPetsByStatus" :: "findById" :: Nil
//  val operations = "allPets" :: Nil
  def listOperations = {
    get("/api-docs/pet.json") {
      val bo = JsonParser.parseOpt(body)
      bo must beSome[JValue] and
        verifyCommon(bo.get) and
        operations.map(verifyOperation(bo.get, _)).reduce(_ and _)
    }
  }

  def verifyCommon(jv: JValue): MatchResult[Any] = {
    (jv \ "apiVersion" must_== listOperationsJValue \ "apiVersion") and
    (jv \ "swaggerVersion" must_== listOperationsJValue \ "swaggerVersion") and
    (jv \ "basePath" must_== listOperationsJValue \ "basePath") and
    (jv \ "description" must_== listOperationsJValue \ "description") and
    (jv \ "resourcePath" must_== listOperationsJValue \ "resourcePath") and {
      val ja = jv \ "apis" \ "path" \\ classOf[JString]
      (ja.size must_== 4) and
        (ja must haveTheSameElementsAs(List("/{id}", "/findByTags", "/findByStatus", "/")))
    }
  }

  def verifyOperation(jv: JValue, name: String) = {
    val op = findOperation(jv, name)
    val exp = findOperation(listOperationsJValue, name)
    (op must beSome[JValue]).setMessage("Couldn't find extractOperation: " + name) and {
      val m = verifyFields(op.get, exp.get, "httpMethod", "nickname", "responseClass", "summary", "parameters", "notes", "errorResponses")
      m setMessage (m.message + " of the extractOperation " + name)
    }
  }

  def verifyFields(actual: JValue, expected: JValue, fields: String*): MatchResult[Any] = {
    def verifyField(act: JValue, exp: JValue, fn: String): MatchResult[Any] = {
      fn match {
        case "errorResponses" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "code" == v \ "code") get,
              "code", "reason")
            mm setMessage (mm.message + " in error responses collection")
          }
          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case "parameters" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "name" == v \ "name") get,
              "allowableValues", "dataType", "paramType", "allowMultiple", "defaultValue", "description", "name", "required")
            mm setMessage (mm.message + " for parameter " + ( v \ "name" ).extractOrElse("N/A"))
          }

          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case _ =>
          val m = act \ fn must_== exp \ fn
          val rdr = (act \ fn) match {
            case JNothing => "<nothing>"
            case jv => JsonMethods.compact(JsonMethods.render(jv))
          }
          m setMessage (rdr + " does not match\n" + rdr + " for field " + fn)
      }
    }

    (fields map (verifyField(actual, expected, _)) reduce (_ and _))
  }

  def findOperation(jv: JValue, name: String) = {
    val JArray(ja) = jv \ "apis"
    ja find { jn =>
      val JArray(ops) = jn \ "operations"
      ops.exists(_ \ "nickname" == JString(name))
    } flatMap { api =>
      (api \ "operations").find(_ \ "nickname" == JString(name))
    }
  }
}