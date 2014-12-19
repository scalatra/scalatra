package org.scalatra.jetty

import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import java.net.{ URL, InetSocketAddress }
import org.scalatra.{ LifeCycle, ScalatraServlet }
import scala.io.Source
import javax.servlet.ServletContext
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.server.ServerConnector

object JettyServerSpec {
  class HelloServlet extends ScalatraServlet {
    get("/") { "hello" }
  }

  class ScalatraBootstrap extends LifeCycle {
    override def init(context: ServletContext): Unit = {
      context.mount(new HelloServlet, "/*")
    }
  }
}

class JettyServerSpec extends WordSpec with BeforeAndAfterAll {
  import JettyServerSpec._
  var jetty: JettyServer = _
  var port: Int = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    jetty = new JettyServer(InetSocketAddress.createUnresolved("localhost", 0))
    jetty.context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[JettyServerSpec.ScalatraBootstrap].getName)
    jetty.start()
    port = jetty.server.getConnectors.head.asInstanceOf[ServerConnector].getLocalPort
  }

  override protected def afterAll(): Unit = {
    jetty.stop()
    super.afterAll()
  }

  "A JettyServer" should {
    "return hello" in {
      val stream = Source.fromInputStream(new URL("http://localhost:" + port + "/").openStream())
      try {
        assert(stream.getLines().mkString === "hello")
      } finally {
        stream.close()
      }
    }
  }
}
