package org.scalatra

class CookiesExample extends ScalatraServlet {
  get("/") {
    val previous = cookies.get("counter") match {
      case Some(v) => v.toInt
      case None => 0
    }
    cookies.update("counter", (previous + 1).toString)
    <p>
      Hi, you have been on this page{ previous }
      times already
    </p>
  }
}
