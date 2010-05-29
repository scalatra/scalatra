package com.thinkminimo.step.fileupload

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.mortbay.jetty.testing.{ServletTester, HttpTester}
import org.apache.commons.io.IOUtils
import com.thinkminimo.step.Step

class FileUploadSupportTestServlet extends Step with FileUploadSupport {
  post("/multipart") {
    params.get("string") foreach { response.setHeader("string", _) }
    fileParams.get("file") foreach { fi => response.setHeader("file", new String(fi.get)) }
    fileParams.get("file-none") foreach { fi => response.setHeader("file-none", new String(fi.get)) }
    fileParams.get("file-multi") foreach { fi => response.setHeader("file-multi", new String(fi.get)) }
    fileMultiParams.get("file-multi") foreach { fis =>
      response.setHeader("file-multi-all", fis.foldLeft(""){ (acc, fi) => acc + new String(fi.get) })
    }
    params.get("file") foreach { response.setHeader("file-as-param", _) }
  }
}

class FileUploadSupportTest extends FunSuite with ShouldMatchers {
  val tester = new ServletTester

  tester.addServlet(classOf[FileUploadSupportTestServlet], "/")
  tester.start()
  val response = {
    val req = IOUtils.toString(getClass.getResourceAsStream("multipart_request.txt"))
    val res = new HttpTester
    res.parse(tester.getResponses(req))
    res
  }

  test("keeps input parameters on multipart request") {
    response.getHeader("string") should equal ("foo")
  }

  test("sets file params") {
    response.getHeader("file") should equal ("one")
  }

  test("sets file param with no bytes when no file is uploaded") {
    response.getHeader("file-none") should equal ("")
  }

  test("sets multiple file params") {
    response.getHeader("file-multi-all") should equal ("twothree")
  }

  test("fileParams returns first input for multiple file params") {
    response.getHeader("file-multi") should equal ("two")
  }

  test("file params are not params") {
    response.getHeader("file-as-param") should equal (null)
  }
}