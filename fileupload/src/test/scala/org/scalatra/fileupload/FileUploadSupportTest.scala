package org.scalatra
package fileupload

import java.net.{URLDecoder, URLEncoder}
import org.scalatest.FunSuite
import org.eclipse.jetty.testing.{ServletTester, HttpTester}
import org.apache.commons.io.IOUtils
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import test.scalatest.ScalatraFunSuite

class FileUploadSupportTestServlet extends ScalatraServlet with FileUploadSupport {
  post("""/multipart.*""".r) {
    multiParams.get("string") foreach { ps: Seq[String] => response.setHeader("string", ps.mkString(";")) }
    fileParams.get("file") foreach { fi => response.setHeader("file", new String(fi.get)) }
    fileParams.get("file-none") foreach { fi => response.setHeader("file-none", new String(fi.get)) }
    fileParams.get("file-two[]") foreach { fi => response.setHeader("file-two", new String(fi.get)) }
    fileMultiParams.get("file-two[]") foreach { fis =>
      response.setHeader("file-two-with-brackets", fis.foldLeft(""){ (acc, fi) => acc + new String(fi.get) })
    }
    fileMultiParams.get("file-two") foreach { fis =>
      response.setHeader("file-two-without-brackets", fis.foldLeft(""){ (acc, fi) => acc + new String(fi.get) })
    }
    params.get("file") foreach { response.setHeader("file-as-param", _) }
    params("utf8-string")
  }

  post("/multipart-pass") {
    pass()
  }

  post("/multipart-param") {
    params.get("queryParam") foreach { p =>
      response.addHeader("Query-Param", p)
    }
    pass()
  }

  post("/echo") {
    params.getOrElse("echo", "")
  }
}

@RunWith(classOf[JUnitRunner])
class FileUploadSupportTest extends ScalatraFunSuite {
  addServlet(classOf[FileUploadSupportTestServlet], "/*")

  def multipartResponse(path: String = "/multipart") = {
    // TODO We've had problems with the tester not running as iso-8859-1, even if the
    // request really isn't iso-8859-1.  This is a hack, but this hack passes iff the
    // browser behavior is correct.
    val req = new String(IOUtils.toString(getClass.getResourceAsStream("multipart_request.txt"))
      .replace("${PATH}", path).getBytes, "iso-8859-1")
    val res = new HttpTester("iso-8859-1")
    res.parse(tester.getResponses(req))
    res
  }

  test("keeps input parameters on multipart request") {
    multipartResponse().getHeader("string") should equal ("foo")
  }

  test("decodes input parameters according to request encoding") {
    multipartResponse().getContent() should equal ("föo")
  }

  test("sets file params") {
    multipartResponse().getHeader("file") should equal ("one")
  }

  test("sets file param with no bytes when no file is uploaded") {
    multipartResponse().getHeader("file-none") should equal ("")
  }

  test("sets multiple file params") {
    multipartResponse().getHeader("file-two-with-brackets") should equal ("twothree")
  }

  test("looks for params with [] suffix, Ruby style") {
    multipartResponse().getHeader("file-two-without-brackets") should equal ("twothree")
  }

  test("fileParams returns first input for multiple file params") {
    multipartResponse().getHeader("file-two") should equal ("two")
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

  test("keeps query parameters") {
    multipartResponse("/multipart-param?queryParam=foo").getHeader("Query-Param") should equal ("foo")
  }

  test("query parameters don't shadow post parameters") {
    multipartResponse("/multipart-param?string=bar").getHeader("string") should equal ("bar;foo")
  }
}
