package org.scalatra.tomcat

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec
import org.scalatra.ServletCompat.ServletContext
import org.scalatra.servlet.ScalatraListener
import org.scalatra.{ LifeCycle, ScalatraServlet }

import java.net.URI
import scala.io.Source

object TomcatServerSpec {
  class HelloServlet extends ScalatraServlet {
    get("/") { "hello" }
  }

  class ScalatraBootstrap extends LifeCycle {
    override def init(context: ServletContext): Unit = {
      context.mount(new HelloServlet, "/*")
    }
  }
}

class TomcatServerSpec extends AnyWordSpec with BeforeAndAfterAll {
  var tomcat: TomcatServer = _
  var port: Int = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    tomcat = new TomcatServer(port = 0, docBase = null)
    tomcat.context.addParameter(ScalatraListener.LifeCycleKey, classOf[TomcatServerSpec.ScalatraBootstrap].getName)
    tomcat.start()
    port = tomcat.server.getConnector.getLocalPort
  }

  override protected def afterAll(): Unit = {
    tomcat.stop()
    super.afterAll()
  }

  "A TomcatServer" should {
    "return hello" in {
      val stream = Source.fromInputStream(new URI("http://localhost:" + port + "/").toURL.openStream())
      try {
        assert(stream.getLines().mkString === "hello")
      } finally {
        stream.close()
      }
    }
  }
}
