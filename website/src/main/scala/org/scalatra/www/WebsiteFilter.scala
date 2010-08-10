package org.scalatra
package www

import scalate.ScalateSupport

class WebsiteFilter 
  extends ScalatraFilter 
  with ScalateSupport
{
  get("/") {
    contentType = "text/html"
    templateEngine.layout("/index.scaml")
  }
}
