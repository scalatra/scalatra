package org.scalatra.test

import org.eclipse.jetty.testing.HttpTester

class ScalatraHttpTester(t: HttpTester) {
  object header {
    def apply(k: String) = t.getHeader(k)
    def update(k: String, v: String) = t.setHeader(k,v)
  }
  def status = t.getStatus
  def body = t.getContent

  def mediaType: Option[String] = Option(header("Content-Type")) map { _.split(";")(0) }

  def charset: Option[String] =
    for {
      ct <- Option(header("Content-Type"))
      charset <- ct.split(";").drop(1).headOption
    } yield { charset.toUpperCase.replace("CHARSET=", "").trim }
}
