package examples

import org.scalatra._

// This Scalatra application is a simple servlet.
class ScalatraExample extends ScalatraServlet {

  // A Scalatra application is made up of routes.  A route is an HTTP
  // method (e.g., GET, POST), a path pattern, and an action block to
  // execute if the route matches.
  //
  // This route matches GET requests to /hello/name, where name is a path
  // parameter.
  get("/hello/:name") {
    // The name parameter is available in the params map.
    val name = params("name")

    // You can do business logic inline or call out to your business tier.
    // Scalatra leaves these architectural decisions to you.
    val nameUpper = name.toUpperCase

    // You have full access to the Servlet API.
    response.setHeader("X-Powered-By", "Scalatra")

    // Like any good MVC framework, Scalatra allows you to separate the
    // logic from the view by calling out to a templating system such
    // as Scalate.
    //
    // Alternatively, Scalatra will render the return type of the action
    // block.  In the case of an XML literal, a content-type of "text/html"
    // is inferred, and the XML is rendered back to the response.  This
    // rendering is completely customizable.
    <h1>Hello, {nameUpper}!</h1>
  }
}