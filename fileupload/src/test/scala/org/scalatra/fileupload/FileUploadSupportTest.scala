package org.scalatra
package fileupload

import org.apache.commons.fileupload.FileUploadBase
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraFunSuite

class FileUploadSupportTestServlet extends ScalatraServlet with FileUploadSupport {
  post("""/multipart.*""".r) {
    multiParams.get("string") foreach { ps: Seq[String] => response.setHeader("string", ps.mkString(";")) }
    fileParams.get("file") foreach { fi => response.setHeader("file", new String(fi.get).trim) }
    fileParams.get("file-none") foreach { fi => response.setHeader("file-none", new String(fi.get).trim) }
    fileParams.get("file-two[]") foreach { fi => response.setHeader("file-two", new String(fi.get).trim) }
    fileMultiParams.get("file-two[]") foreach { fis =>
      response.setHeader("file-two-with-brackets", fis.foldLeft("") { (acc, fi) => acc + new String(fi.get).trim })
    }
    fileMultiParams.get("file-two") foreach { fis =>
      response.setHeader("file-two-without-brackets", fis.foldLeft("") { (acc, fi) => acc + new String(fi.get).trim })
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

class MaxSizeTestServlet extends ScalatraServlet with FileUploadSupport {
  post() {
  }

  error {
    case e: FileUploadBase.SizeLimitExceededException => halt(413, "boom")
  }

  override def newServletFileUpload = {
    val upload = super.newServletFileUpload
    upload.setSizeMax(1)
    upload
  }
}

@RunWith(classOf[JUnitRunner])
class FileUploadSupportTest extends ScalatraFunSuite {
  addServlet(classOf[FileUploadSupportTestServlet], "/*")
  addServlet(classOf[MaxSizeTestServlet], "/max-size/*")

  def multipartResponse(path: String = "/multipart") = {
    val reqBody = new String(
      IOUtils.toString(getClass.getResourceAsStream("multipart_request.txt")).getBytes, "iso-8859-1").getBytes("iso-8859-1")

    val boundary = "---------------------------3924013385056820061124200860"

    post(path, headers = Map("Content-Type" -> "multipart/form-data; boundary=%s".format(boundary)), body = reqBody) {
      response
    }
  }

  //  test("keeps input parameters on multipart request") {
  //    multipartResponse().getHeader("string") should equal ("foo")
  //  }
  //
  //  test("decodes input parameters according to request encoding") {
  //    multipartResponse().getContent() should equal ("föo")
  //  }

  test("sets file params") {
    val out = multipartResponse().getHeader("file").toCharArray.map(_.toByte).toList
    println(s"the output of file params: $out")
    multipartResponse().getHeader("file") should equal("one")
  }

  test("sets file param with no bytes when no file is uploaded") {
    multipartResponse().getHeader("file-none") should equal("")
  }

  test("sets multiple file params") {
    multipartResponse().getHeader("file-two-with-brackets") should equal("twothree")
  }

  test("looks for params with [] suffix, Ruby style") {
    multipartResponse().getHeader("file-two-without-brackets") should equal("twothree")
  }

  test("fileParams returns first input for multiple file params") {
    multipartResponse().getHeader("file-two") should equal("two")
  }

  test("file params are not params") {
    multipartResponse().getHeader("file-as-param") should equal(null)
  }

  test("keeps input params on pass") {
    multipartResponse("/multipart-pass").getHeader("string") should equal("foo")
  }

  test("keeps file params on pass") {
    multipartResponse("/multipart-pass").getHeader("file") should equal("one")
  }

  test("reads form params on non-multipart request") {
    post("/echo", "echo" -> "foo") {
      body should equal("foo")
    }
  }

  test("keeps query parameters") {
    multipartResponse("/multipart-param?queryParam=foo").getHeader("Query-Param") should equal("foo")
  }

  test("query parameters don't shadow post parameters") {
    multipartResponse("/multipart-param?string=bar").getHeader("string") should equal("bar;foo")
  }

  test("max size is respected") {
    multipartResponse("/max-size/").status should equal(413)
  }

  test("file upload exceptions are handled by standard error handler") {
    multipartResponse("/max-size/").body should equal("boom")
  }
}
