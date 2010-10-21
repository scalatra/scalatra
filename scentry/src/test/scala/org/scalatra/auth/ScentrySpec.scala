package org.scalatra.auth

import org.specs._
import mock.Mockito
import org.mockito.Matchers._
import runner.{ScalaTest, JUnit}
import javax.servlet.http.{Cookie, HttpServletResponse, HttpServletRequest, HttpSession}
import org.scalatra.SweetCookies

object ScentrySpec extends Specification with Mockito with JUnit with ScalaTest {
  detailedDiffs
  case class User(id: String)

  "The scentry" should {

    val session = smartMock[HttpSession]
    session.getAttribute(Scentry.scentryAuthKey) returns "6789"
    var invalidateCalled = false
    session.invalidate answers { _ => invalidateCalled = true }
    val response = smartMock[HttpServletResponse]
    val req = smartMock[HttpServletRequest]
    req.getCookies returns Array[Cookie]()
    val context = ScalatraKernelProxy(
      session,
      smartMock[scala.collection.Map[String, String]],
      s => "redirected to: " + s,
      req,
      response,
      new SweetCookies(req.getCookies, response))
    val theScentry = new Scentry[User](context, { case User(id) => id }, { case s: String => User(s)})
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
        def authenticate_! ={
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
        override def afterAuthenticate(winningStrategy: Symbol, user: User) = afterAuthenticateCalled = true
      }

    val sUnsuccess = new ScentryStrategy[User] {
        protected val app = context
        def authenticate_! = {
          failingStrategyCalled = true
          None
        }
        override def beforeAuthenticate = beforeAuthenticateCalled = true
        override def afterAuthenticate(winningStrategy: Symbol, user: User) = afterAuthenticateCalled = true
      }
    "allow registration of global strategies" in {
      Scentry.registerStrategy('Bogus, app =>  s)
      Scentry.globalStrategies('Bogus).asInstanceOf[Scentry[User]#StrategyFactory](context) must be_==(s)
    }

    "allow registration of local strategies" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.strategies('LocalFoo) must be_==(s)
    }

    "return both global and local strategies from instance" in {
      Scentry.registerStrategy('Bogus, app =>  s)
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.strategies.size must be_==(2)
    }

    "run all fetch user callbacks" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.user must be_==(User("6789"))
      beforeFetchCalled must be_==(true)
      afterFetchCalled must be_==(true)
    }

    "run all set user callbacks" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      (theScentry.user = User("6789")) must be_==("6789")
      beforeSetUserCalled must be_==(true)
      afterSetUserCalled must be_==(true)
    }

    "run all logout callbacks" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.logout
      beforeLogoutCalled must be_==(true)
      afterLogoutCalled must be_==(true)
      invalidateCalled must be_==(true)
    }

    "run all login callbacks on successful authentication" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.authenticate()
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(true)
      beforeSetUserCalled must be_==(true)
      afterSetUserCalled must be_==(true)
    }

    "run only the before authentication on unsuccessful authentication" in {
      theScentry.registerStrategy('LocalBar, app => sUnsuccess)
      theScentry.authenticate()
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(false)
    }

    "run only the strategy specified by the name" in {
      theScentry.registerStrategy('LocalFoo, app => s)
      theScentry.registerStrategy('LocalBar, app => sUnsuccess)
      theScentry.authenticate('LocalBar)
      beforeAuthenticateCalled must be_==(true)
      afterAuthenticateCalled must be_==(false)
      failingStrategyCalled must be_==(true)
      successStrategyCalled must be_==(false)
    }
  }
}
