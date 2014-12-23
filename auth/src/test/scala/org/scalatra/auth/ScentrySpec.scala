package org.scalatra
package auth

import javax.servlet.http._

import org.mockito.Matchers._
import org.scalatra.auth.ScentryAuthStore.SessionAuthStore
import org.specs2.mock.Mockito
import org.specs2.mutable._

object ScentrySpec extends Specification with Mockito {
  sequential
  isolated

  case class User(id: String)

  "The scentry" should {

    var invalidateCalled = false
    val context = new ScalatraFilter {
      private[this] val sessionMap = scala.collection.mutable.HashMap[String, Any](Scentry.scentryAuthKey -> "6789")
      val mockSession = smartMock[HttpSession]
      override def session(implicit request: HttpServletRequest) = mockSession
      mockSession.getAttribute(anyString) answers { k => sessionMap.getOrElse(k.asInstanceOf[String], null).asInstanceOf[AnyRef] }
      mockSession.setAttribute(anyString, anyObject) answers { (kv, wtfIsThis) =>
        val Array(k: String, v: Any) = kv
        sessionMap(k) = v
      }
      mockSession.invalidate() answers { k =>
        invalidateCalled = true
        sessionMap.clear()
      }
    }

    implicit val req = mock[HttpServletRequest]

    implicit val res = mock[HttpServletResponse].smart

    val theScentry = new Scentry[User](context, { case User(id) => id }, { case s: String => User(s) }, new SessionAuthStore(context))
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
    var unauthenticatedCalled = false
    var unauthenticatedSuccessCalled = false
    var defaultUnauthenticatedCalled = false

    Scentry.clearGlobalStrategies
    theScentry unauthenticated {
      defaultUnauthenticatedCalled = true
    }

    val s = new ScentryStrategy[User] {
      protected val app = context
      def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = {
        successStrategyCalled = true
        Some(User("12345"))
      }
      override def beforeFetch[IdType](id: IdType)(implicit request: HttpServletRequest, response: HttpServletResponse) = beforeFetchCalled = true
      override def afterFetch(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = afterFetchCalled = true
      override def beforeSetUser(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = beforeSetUserCalled = true
      override def afterSetUser(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = afterSetUserCalled = true
      override def beforeLogout(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = beforeLogoutCalled = true
      override def afterLogout(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = afterLogoutCalled = true
      override def beforeAuthenticate(implicit request: HttpServletRequest, response: HttpServletResponse) = beforeAuthenticateCalled = true
      override def afterAuthenticate(winningStrategy: String, user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = afterAuthenticateCalled = true
      override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) { unauthenticatedSuccessCalled = true }
    }

    val sUnsuccess = new ScentryStrategy[User] {
      protected val app = context
      def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = {
        failingStrategyCalled = true
        None
      }
      override def beforeAuthenticate(implicit request: HttpServletRequest, response: HttpServletResponse) = beforeAuthenticateCalled = true
      override def afterAuthenticate(winningStrategy: String, user: User)(implicit request: HttpServletRequest, response: HttpServletResponse) = afterAuthenticateCalled = true
      override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) { unauthenticatedCalled = true }
    }
    "allow registration of global strategies" in {
      Scentry.register("Bogus", (_: ScalatraBase) => s)
      Scentry.globalStrategies("Bogus").asInstanceOf[Scentry[User]#StrategyFactory](context) must be_==(s)
    }

    "allow registration of local strategies" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.strategies("LocalFoo") must be_==(s)
    }

    "return both global and local strategies from instance" in {
      Scentry.register("Bogus", _ => s)
      theScentry.register("LocalFoo", app => s)
      theScentry.strategies.size must be_==(2)
    }

    "run all fetch user callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      req.getAttribute("scentry.auth.default.user") returns null
      theScentry.user must be_==(User("6789"))
      beforeFetchCalled must beTrue
      afterFetchCalled must beTrue
    }

    "run all set user callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      req.getAttribute("scentry.auth.default.user") returns null
      (theScentry.user = User("6789")) must be_==("6789")
      req.getAttribute("scentry.auth.default.user") returns User("6789")
      there was atLeastOne(req).setAttribute("scentry.auth.default.user", User("6789"))
      beforeSetUserCalled must beTrue
      afterSetUserCalled must beTrue
    }

    "run all logout callbacks" in {
      theScentry.register("LocalFoo", _ => s)
      req.getAttribute("scentry.auth.default.user") returns User("6789")
      theScentry.logout
      there was one(req).removeAttribute("scentry.auth.default.user")
      beforeLogoutCalled must beTrue
      afterLogoutCalled must beTrue
      invalidateCalled must beTrue
    }

    "run all login callbacks on successful authentication" in {
      theScentry.register("LocalFoo", _ => s)
      req.getAttribute("scentry.auth.default.user") returns null
      theScentry.authenticate()
      there were two(req).setAttribute("scentry.auth.default.user", User("12345"))
      req.getAttribute("scentry.auth.default.user") returns User("12345")
      beforeAuthenticateCalled must beTrue
      afterAuthenticateCalled must beTrue
      beforeSetUserCalled must beTrue
      afterSetUserCalled must beTrue
    }

    "run only the before authentication on unsuccessful authentication" in {
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate()
      beforeAuthenticateCalled must beTrue
      afterAuthenticateCalled must beFalse
    }

    "run only the strategy specified by the name" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate("LocalBar")
      beforeAuthenticateCalled must beTrue
      afterAuthenticateCalled must beFalse
      failingStrategyCalled must beTrue
      successStrategyCalled must beFalse
    }

    "run the unauthenticated hook when authenticating by name" in {
      theScentry.register("LocalFoo", _ => s)
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate("LocalBar")
      beforeAuthenticateCalled must beTrue
      afterAuthenticateCalled must beFalse
      failingStrategyCalled must beTrue
      unauthenticatedCalled must beTrue
      unauthenticatedSuccessCalled must beFalse
      defaultUnauthenticatedCalled must beFalse
    }

    "run the default unauthenticated hook when authenticating without a name" in {
      //      theScentry.register("LocalFoo", _ => s)
      theScentry.register("LocalBar", _ => sUnsuccess)
      theScentry.authenticate()
      unauthenticatedCalled must beTrue
      unauthenticatedSuccessCalled must beFalse
      defaultUnauthenticatedCalled must beTrue
    }
  }
}
