package org.scalatra

import docs._

class DocumentExample extends ScalatraServlet with DocumentationSupport {

  val helloParam = Param(name = "hello", description = "The hello parameter")
  val helloDoc = Documentation(name = "hello world", route = "/hello", description = "The hello world documentation example", requiredParams = List(helloParam), optionalParams = List(helloParam))

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