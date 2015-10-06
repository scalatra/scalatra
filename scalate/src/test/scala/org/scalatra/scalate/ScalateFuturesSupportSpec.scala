package org.scalatra.scalate

import java.util.concurrent.{ ExecutorService, Executors, ThreadFactory }

import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.scalatra._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.specs2.specification._

import scala.concurrent.{ ExecutionContext, Future }

class DaemonThreadFactory extends ThreadFactory {
  def newThread(r: Runnable): Thread = {
    val thread = new Thread(r)
    thread setDaemon true
    thread
  }
}

object DaemonThreadFactory {
  def newPool() = Executors.newCachedThreadPool(new DaemonThreadFactory)
}

class ScalateFuturesSupportServlet(exec: ExecutorService) extends ScalatraServlet with ScalateSupport with ScalateUrlGeneratorSupport with FlashMapSupport with FutureSupport {
  protected implicit val executor = ExecutionContext.fromExecutorService(exec)

  get("/barf") {
    new AsyncResult { val is = Future { throw new RuntimeException } }
  }

  get("/happy-happy") {
    new AsyncResult { val is = Future { "puppy dogs" } }
  }

  get("/simple-template") {
    new AsyncResult { val is = Future { layoutTemplate("/simple.jade") } }
  }

  get("/params") {
    new AsyncResult { val is = Future { layoutTemplate("/params.jade", "foo" -> "Configurable") } }
  }

  get("/jade-template") {
    new AsyncResult { val is = Future { jade("simple") } }
  }

  get("/jade-params") {
    new AsyncResult { val is = Future { jade("params", "foo" -> "Configurable") } }
  }

  get("/scaml-template") {
    new AsyncResult { val is = Future { scaml("simple") } }
  }

  get("/scaml-params") {
    new AsyncResult { val is = Future { scaml("params", "foo" -> "Configurable") } }
  }

  get("/ssp-template") {
    new AsyncResult { val is = Future { ssp("simple") } }
  }

  get("/ssp-params") {
    new AsyncResult { val is = Future { ssp("params", "foo" -> "Configurable") } }
  }

  get("/mustache-template") {
    new AsyncResult { val is = Future { mustache("simple") } }
  }

  get("/mustache-params") {
    new AsyncResult { val is = Future { mustache("params", "foo" -> "Configurable") } }
  }

  get("/layout-strategy") {
    new AsyncResult { val is = Future { templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts mkString ";" } }
  }

  val urlGeneration = get("/url-generation") {
    new AsyncResult { val is = Future { layoutTemplate("/urlGeneration.jade") } }
  }

  val urlGenerationWithParams = get("/url-generation-with-params/:a/vs/:b") {

    new AsyncResult {
      val is = Future {
        println("Rendering reverse routing template")
        layoutTemplate("/urlGenerationWithParams.jade", ("a" -> params("a")), ("b" -> params("b")))
      }
    }
  }

  get("/legacy-view-path") {
    new AsyncResult { val is = Future { jade("legacy") } }
  }

  get("/directory") {
    new AsyncResult { val is = Future { jade("directory/index") } }
  }

  get("/bindings/*") {
    new AsyncResult {
      val is =
        Future {
          flash.now("message") = "flash works"
          session("message") = "session works"
          jade(requestPath)
        }
    }
  }

  get("/bindings/params/:foo") {
    new AsyncResult { val is = Future { jade("/bindings/params") } }
  }

  get("/bindings/multiParams/*/*") {
    new AsyncResult { val is = Future { jade("/bindings/multiParams") } }
  }

  get("/template-attributes") {
    new AsyncResult {
      val is =
        Future {
          templateAttributes("foo") = "from attributes"
          scaml("params")
        }
    }
  }

  get("/render-to-string") {
    new AsyncResult { val is = Future { response.setHeader("X-Template-Output", layoutTemplate("simple")) } }
  }
}

class ScalateFuturesSupportSpec extends MutableScalatraSpec {
  sequential
  "ScalateSupport with Futures" should {
    "render uncaught errors with 500.scaml" in e1
    "not throw a NullPointerException for trivial requests" in e2
    "render a simple template" in e3
    "render a simple template with params" in e4
    "looks for layouts in /WEB-INF/layouts" in e5
    "generate a url from a template" in e6
    "generate a url with params from a template" in e7
    "render a simple template via jade method" in e8
    "render a simple template with params via jade method" in e9
    "render a simple template via scaml method" in e10
    "render a simple template with params via scaml method" in e11
    "render a simple template via ssp method" in e12
    "render a simple template with params via ssp method" in e13
    "render a simple template via mustache method" in e14
    "render a simple template with params via mustache method" in e15
    "looks for templates in legacy /WEB-INF/scalate/templates" in e16
    "looks for index page if no template found" in e17
    "implicitly bind flash" in e18
    "implicitly bind session" in e19
    "implicitly bind params" in e20
    "implicitly bind multiParams" in e21
    "set templateAttributes when creating a render context" in e22
    "render to a string instead of response" in e23
  }

  val pool = DaemonThreadFactory.newPool()
  addServlet(new ScalateFuturesSupportServlet(pool), "/*")

  override def afterAll = {
    super.afterAll
    pool.shutdown()
  }

  def e1 = get("/barf") {
    status must_== 500
    body must contain("id=\"scalate-error\"")
  }

  def e2 = get("/happy-happy") {
    body must_== "puppy dogs"
  }

  def e3 = get("/simple-template") {
    body must_== "<div>Jade template</div>\n"
  }

  def e4 = get("/params") {
    body must_== "<div>Configurable template</div>\n"
  }

  // Testing the default layouts is going to be hard, but we can at least
  // verify that it's looking in the right place.
  def e5 = get("/layout-strategy") {
    body must_== (List(
      "/WEB-INF/templates/layouts/default.mustache",
      "/WEB-INF/templates/layouts/default.ssp",
      "/WEB-INF/templates/layouts/default.scaml",
      "/WEB-INF/templates/layouts/default.jade",
      "/WEB-INF/layouts/default.mustache",
      "/WEB-INF/layouts/default.ssp",
      "/WEB-INF/layouts/default.scaml",
      "/WEB-INF/layouts/default.jade",
      "/WEB-INF/scalate/layouts/default.mustache",
      "/WEB-INF/scalate/layouts/default.ssp",
      "/WEB-INF/scalate/layouts/default.scaml",
      "/WEB-INF/scalate/layouts/default.jade"
    ) mkString ";")
  }

  def e6 = get("/url-generation") {
    body must_== "/url-generation\n"
  }

  def e7 = {
    println("reverse route params")
    get("/url-generation-with-params/jedi/vs/sith") {
      body must_== "/url-generation-with-params/jedi/vs/sith\n"
    }
  }

  def e8 = get("/jade-template") {
    body must_== "<div>Jade template</div>\n"
  }

  def e9 = get("/jade-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e10 = get("/scaml-template") {
    body must_== "<div>Scaml template</div>\n"
  }

  def e11 = get("/scaml-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e12 = get("/ssp-template") {
    body must_== "<div>SSP template</div>"
  }

  def e13 = get("/ssp-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e14 = get("/mustache-template") {
    body must_== "<div>Mustache template</div>\n"
  }

  def e15 = get("/mustache-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e16 = get("/legacy-view-path") {
    body must_== "<p>legacy</p>\n"
  }

  def e17 = get("/directory") {
    body must_== "<p>index</p>\n"
  }

  def e18 = get("/bindings/flash") {
    body must_== "<div>flash works</div>\n"
  }

  def e19 = get("/bindings/session") {
    body must_== "<div>session works</div>\n"
  }

  def e20 = get("/bindings/params/bar") {
    body must_== "<div>bar</div>\n"
  }

  def e21 = get("/bindings/multiParams/bar/baz") {
    body must_== "<div>bar;baz</div>\n"
  }

  def e22 = get("/template-attributes") {
    body must_== "<div>from attributes template</div>\n"
  }

  def e23 = get("/render-to-string") {
    header("X-Template-Output") must_== "<div>SSP template</div>"
  }
}
