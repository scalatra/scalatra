package org.scalatra

import auth.strategy.{BasicAuthStrategy, BasicAuthSupport}
import auth.{ScentrySupport, ScentryConfig}
import servlet.{ServletBase}

import org.scalatra.BasicAuthExample.AuthenticationSupport

object BasicAuthExample {

  case class MyUser(id: String)

  class OurBasicAuthStrategy(protected override val app: ServletBase, realm: String)
    extends BasicAuthStrategy[MyUser](app, realm) {

    protected def validate(userName: String, password: String): Option[MyUser] = {
      if(userName == "scalatra" && password == "scalatra") Some(MyUser("scalatra"))
      else None
    }

    protected def getUserId(user: MyUser): String = user.id
  }

  trait AuthenticationSupport extends ScentrySupport[MyUser] with BasicAuthSupport[MyUser] { 
    self: ServletBase =>

    val realm = "Scalatra Basic Auth Example"

    protected def fromSession = { case id: String => MyUser(id)  }
    protected def toSession   = { case usr: MyUser => usr.id }

    protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]


    override protected def configureScentry = {
      scentry.unauthenticated {
        scentry.strategies('Basic).unauthenticated()
      }
    }

    override protected def registerAuthStrategies = {

      scentry.registerStrategy('Basic, app => new OurBasicAuthStrategy(app, realm))
    }

  }
}

class BasicAuthExample extends ScalatraServlet with AuthenticationSupport {
  get("/?") {
    basicAuth
    <html>
      <body>
        <h1>Hello from Scalatra</h1>
        <p><a href="/auth/linked" >click</a></p>
      </body>
    </html>
  }

  get("/linked") {
    basicAuth
    <html>
      <body>
        <h1>Hello again from Scalatra</h1>
        <p><a href="/" >back</a></p>
      </body>
    </html>
  }
}
