package org.scalatra.jetty

import java.net.{ InetSocketAddress, URI }
import org.scalatra.ServletCompat.ServletContext

import org.eclipse.jetty.server.ServerConnector
import org.scalatest.BeforeAndAfterAll
import org.scalatra.servlet.ScalatraListener
import org.scalatra.{ LifeCycle, ScalatraServlet }

import scala.io.Source
import org.scalatest.wordspec.AnyWordSpec

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

class JettyServerSpec extends AnyWordSpec with BeforeAndAfterAll {
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
      val stream = Source.fromInputStream(new URI("http://localhost:" + port + "/").toURL.openStream())
      try {
        assert(stream.getLines().mkString === "hello")
      } finally {
        stream.close()
      }
    }
  }
}
