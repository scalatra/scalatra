package org.scalatra

import docs._

class DocumentExample extends ScalatraServlet with DocumentationSupport {

  val helloDoc = Documentation(name = "hello world", route = "/hello")

  before(){
    contentType = "text/html"
  }

  get(helloDoc.route, doc(helloDoc)) {
    "hello"
  }

  get("/render") {
    docsAsHtml()
  }


}