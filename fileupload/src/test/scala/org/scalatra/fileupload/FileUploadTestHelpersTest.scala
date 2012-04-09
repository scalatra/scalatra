package org.scalatra.fileupload

import scala.collection.JavaConversions._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.ScalatraServlet
import java.io.File

class FileUploadTestHelpersTestServlet extends ScalatraServlet with FileUploadSupport {
  def handleRequest() {
    response.setHeader("Request-Method", request.getMethod)
    params.foreach(p => response.setHeader("Param-" + p._1, p._2))

    request.getHeaderNames.filter(header => header.startsWith("Test-")).foreach(header =>
      response.setHeader(header, request.getHeader(header))
    )

    fileParams.foreach(fileParam => {
      response.setHeader("File-" + fileParam._1 + "-Name", fileParam._2.name)
      response.setHeader("File-" + fileParam._1 + "-Size", fileParam._2.size.toString)
      response.setHeader("File-" + fileParam._1 + "-SHA", DigestUtils.shaHex(fileParam._2.bytes))
    })
  }

  post("/") {
    handleRequest()

    "OK"
  }

  put("/") {
    handleRequest()

    "OK"
  }
}

class FileUploadTestHelpersTest extends ScalatraFunSuite {
  addServlet(classOf[FileUploadTestHelpersTestServlet], "/*")

  val files  = Map(
    "textFile"   -> new File("fileupload/src/test/resources/org/scalatra/fileupload/lorem_ipsum.txt"),
    "binaryFile" -> new File("fileupload/src/test/resources/org/scalatra/fileupload/smiley.png")
  )

  val params = Map("one" -> "1", "two" -> "2")
  val headers = Map("Test-Something" -> "lorem ipsum")

  test("post with files does POST request") {
    post("/", params, files, headers) {
      assert(header("Request-Method") === "POST")
    }
  }

  test("post with files parameters and headers are accessible from servlet") {
    post("/", params, files, headers) {
      assert(header("Test-Something") === "lorem ipsum")
      assert(header("Param-one") === "1")
      assert(header("Param-two") === "2")
    }
  }

  test("post with files makes the files available from servlet") {
    post("/", params, files, headers) {
      assert(header("File-textFile-Name") === "lorem_ipsum.txt")
      assert(header("File-textFile-Size") === "651")
      assert(header("File-textFile-SHA") === "b3572a890c5005aed6409cf81d13fd19f6d004f0")

      assert(header("File-binaryFile-Name") === "smiley.png")
      assert(header("File-binaryFile-Size") === "3432")
      assert(header("File-binaryFile-SHA") === "0e777b71581c631d056ee810b4550c5dcd9eb856")
    }
  }

  test("put with files does PUT request") {
    put("/", params, files, headers) {
      assert(header("Request-Method") === "PUT")
    }
  }

  test("put with files parameters and headers are accessible from servlet") {
    put("/", params, files, headers) {
      assert(header("Test-Something") === "lorem ipsum")
      assert(header("Param-one") === "1")
      assert(header("Param-two") === "2")
    }
  }

  test("put with files makes the files available from servlet") {
    put("/", params, files, headers) {
      assert(header("File-textFile-Name") === "lorem_ipsum.txt")
      assert(header("File-textFile-Size") === "651")
      assert(header("File-textFile-SHA") === "b3572a890c5005aed6409cf81d13fd19f6d004f0")

      assert(header("File-binaryFile-Name") === "smiley.png")
      assert(header("File-binaryFile-Size") === "3432")
      assert(header("File-binaryFile-SHA") === "0e777b71581c631d056ee810b4550c5dcd9eb856")
    }
  }
}