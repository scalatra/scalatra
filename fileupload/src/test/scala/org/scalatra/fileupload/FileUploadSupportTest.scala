package org.scalatra
package fileupload

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.eclipse.jetty.testing.{ServletTester, HttpTester}
import org.apache.commons.io.IOUtils
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import test.scalatest.ScalatraFunSuite

class FileUploadSupportTestServlet extends ScalatraServlet with FileUploadSupport {
  post("""/multipart.*""".r) {
    params.get("string") foreach { response.setHeader("string", _) }
    fileParams.get("file") foreach { fi => response.setHeader("file", new String(fi.get)) }
    fileParams.get("file-none") foreach { fi => response.setHeader("file-none", new String(fi.get)) }
    fileParams.get("file-multi") foreach { fi => response.setHeader("file-multi", new String(fi.get)) }
    fileMultiParams.get("file-multi") foreach { fis =>
      response.setHeader("file-multi-all", fis.foldLeft(""){ (acc, fi) => acc + new String(fi.get) })
    }
    params.get("file") foreach { response.setHeader("file-as-param", _) }
  }

  post("/multipart-pass") {
    println("PASSING")
    pass()
  }

  post("/echo") {
    params.getOrElse("echo", "")
  }
}

@RunWith(classOf[JUnitRunner])
class FileUploadSupportTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[FileUploadSupportTestServlet], "/")

  def multipartResponse(path: String = "/multipart") = {
    val req = IOUtils.toString(getClass.getResourceAsStream("multipart_request.txt"))
      .replace("${PATH}", path)
    val res = new HttpTester("iso-8859-1")
    res.parse(tester.getResponses(req))
    res
  }

  test("keeps input parameters on multipart request") {
    multipartResponse().getHeader("string") should equal ("foo")
  }

  test("sets file params") {
    multipartResponse().getHeader("file") should equal ("one")
  }

  test("sets file param with no bytes when no file is uploaded") {
    multipartResponse().getHeader("file-none") should equal ("")
  }

  test("sets multiple file params") {
    multipartResponse().getHeader("file-multi-all") should equal ("twothree")
  }

  test("fileParams returns first input for multiple file params") {
    multipartResponse().getHeader("file-multi") should equal ("two")
  }

  test("file params are not params") {
    multipartResponse().getHeader("file-as-param") should equal (null)
  }

  test("keeps input params on pass") {
    multipartResponse("/multipart-pass").getHeader("string") should equal ("foo")
  }

  test("keeps file params on pass") {
    multipartResponse("/multipart-pass").getHeader("file") should equal ("one")
  }

  test("reads form params on non-multipart request") {
    post("/echo", "echo" -> "foo") {
      body should equal("foo")
    }
  }
}
