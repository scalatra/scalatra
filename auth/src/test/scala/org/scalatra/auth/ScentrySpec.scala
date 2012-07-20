package org.scalatra
package auth

import org.specs._
import mock.Mockito
import org.mockito.Matchers._
import runner.{ScalaTest, JUnit}
import javax.servlet.http.{Cookie, HttpServletResponse, HttpServletRequest, HttpSession}
import org.scalatra.SweetCookies
import auth.ScentryAuthStore.SessionAuthStore

object ScentrySpec extends Specification with Mockito with JUnit with ScalaTest {
  detailedDiffs
  case class User(id: String)

  "The scentry" should {

    var invalidateCalled = false
    val context = new ScalatraFilter {
      private val sessionMap = scala.collection.mutable.HashMap[String, Any](Scentry.scentryAuthKey -> "6789")
      override val session = smartMock[HttpSession]
      session.getAttribute(anyString) answers { k => sessionMap.getOrElse(k.asInstanceOf[String], null).asInstanceOf[AnyRef] }
      session.setAttribute(anyString(), anyObject()) answers { kv =>
        val kvArray = kv.asInstanceOf[Array[AnyRef]]
        sessionMap(kvArray(0).asInstanceOf[String]) = kvArray(1)
      }
      session.invalidate() answers {
        invalidateCalled = true
        k => sessionMap.clear()
      }
    }
    val theScentry = new Scentry[User](context, { case User(id) => id }, { case s: String => User(s)}, new SessionAuthStore(context.session))
    var beforeFetchCalled = false
    var afterFetchCalled = false
    var beforeSetUserCalled = false
    var afterSetUserCalled = false
    var beforeLogoutCalled = false
    var afterLogoutCalled = false
    var beforeAuthenticateCalled = false
    var afterAuthenticateCalled = false
    var successStrategyCalled = false
    var failingStrategyCalled = false
    Scentry.clearGlobalStrategies

    val s = new ScentryStrategy[User] {
        protected val app = context
        def authenticate() ={
          successStrategyCalled = true
          Some(User("12345"))
        }
        override def beforeFetch[IdType](id: IdType) = beforeFetchCalled = true
        override def afterFetch(user: User) = afterFetchCalled = true
        override def beforeSetUser(user: User) = beforeSetUserCalled = true
        override def afterSetUser(user: User) = afterSetUserCalled = true
        override def beforeLogout(user: User) = beforeLogoutCalled = true
        override def afterLogout(user: User) = afterLogoutCalled = true
        override def beforeAuthenticate = beforeAuthenticateCalled = true
        override def afterAuthenticate(winningStrategy: String, user: User) = afterAuthenticateCalled = true
      }

    val sUnsuccess = new ScentryStrategy[User] {
        protected val app = context
        def authenticate() = {
          failingStrategyCalled = true
          None
        }
        override def beforeAuthenticate = beforeAuthenticateCalled = true
        override def afterAuthenticate(winningStrategy: String, user: User) = afterAuthenticateCalled = true
      }
    "allow registration of global strategies" in {
      Scentry.register("Bogus", (_: ScalatraBase) =>  s)
      Scentry.globalStrategies("Bogus").asInstanceOf[Scentry[User]#StrategyFactory](context) must be_==(s)
    }

    "allow registration of local strategies" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.strategies("LocalFoo") must be_==(s)
    }

    "return both global and local strategies from instance" in {
      Scentry.register("Bogus", _ =>  s)
      theScentry.register("LocalFoo", app => s)
      theScentry.strategies.size must be_==(2)
    }

    "run all fetch user callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.user must be_==(User("6789"))
      beforeFetchCalled must be_==(true)
      afterFetchCalled must be_==(true)
    }

    "run all set user callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      (theScentry.user = User("6789")) must be_==("6789")
      beforeSetUserCalled must be_==(true)
      afterSetUserCalled must be_==(true)
    }

    "run all logout callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.logout
      beforeLogoutCalled must be_==(true)
      afterLogoutCalled must be_==(true)
      invalidateCalled must be_==(true)
    }

    "run all login callbacks on successful authentication" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.authenticate()
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(true)
      beforeSetUserCalled must be_==(true)
      afterSetUserCalled must be_==(true)
    }

    "run only the before authentication on unsuccessful authentication" in {
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate()
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(false)
    }

    "run only the strategy specified by the name" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate("LocalBar")
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(false)
      failingStrategyCalled must be_==(true)
      successStrategyCalled must be_==(false)
    }
  }
}
