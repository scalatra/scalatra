package org.scalatra

import org.specs2.mutable.Specification
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

/**
 * @author Bryce Anderson
 *         Created on 2/23/14
 */
class DSLMacrosSpec extends Specification {

  implicit def string2RouteMatcher(path: String): RouteMatcher = new SinatraRouteMatcher(path)

  val route = new ScalatraServlet {
    override def addRoute(method: HttpMethod,
                           transformers: Seq[RouteTransformer],
                           action: (HttpServletRequest, HttpServletResponse) => Any): Route = null

    def param(name: String)(implicit req: HttpServletRequest) = "foo"

    get("/hello"){
      val r = request
      val resp = response

      val p = param("foo")

      "hello world"
    }

//    val r = request
  }

  "DSLMacros" should {
    "say hello" in {
      true should be equalTo(true)
    }
  }
}
