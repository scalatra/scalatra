package org.scalatra

import _root_.akka.util.{ Duration => AkkaDuration }
import java.util.concurrent.TimeUnit
import org.specs2.specification.After
import org.specs2.{ScalaCheck, Specification}
import org.scalacheck.{Prop, Gen}
import org.scalatra.store.session.InMemorySessionStore
import org.scalacheck.Gen
import org.scalacheck.Prop


class InMemorySessionStoreSpec extends Specification with ScalaCheck { def is =

  "An InMemorySessionStore should" ^
    "store values" ! specify().storesValues ^
    "retrieve values if they are not expired" ! specify().retrievesUnexpired ^
    "expires entries after the specified timeout" ! specify().expiresEntries ^
    "does not fall down under heavy load" ! specify(120).handlesLoad ^
  end


  private def specify(ttl: Int = 1) = new StoreContext(ttl)

  private class StoreContext(ttl: Int) extends After {
    val store = new InMemorySessionStore(AkkaDuration(ttl, TimeUnit.SECONDS))
    implicit val appContext = {
      val ctxt = new AppContext {
        def server = null

        implicit def applications = null

        implicit def appContext = this

        val sessions: SessionStore[_ <: HttpSession] = store
        sessions.initialize(this)
      }
      ctxt.sessionTimeout = AkkaDuration(ttl, TimeUnit.SECONDS)
      ctxt
    }



    def after = {
      store.stop
    }

    def storesValues = this {
      val session = store.newSession
      session("foo") = "bar"
      store(session.id)("foo") must_== "bar"
    }

    def retrievesUnexpired = this {
      val session = store.newSession
      session("foo2") = "bar"
      Thread.sleep(400)
      store(session.id)("foo2") must_== "bar"
    }

    def expiresEntries = this {
      val session = store.newSession
      store get session.id must beSome[HttpSession] and {
        Thread.sleep(1100)
        store get (session.id) must beNone
      }
    }

    def handlesLoad = this {
      val sessions = (1 to 500) map { _ => GenerateId()}
      val values = for {
        k <- Gen.alphaStr.filter(_.nonBlank).map(_ + System.nanoTime.toString)
        v <- Gen.alphaStr.filter(_.nonBlank) } yield k -> v

      println("running scalacheck this will take a while")
      sessions.par map { sessionId =>
        store.newSessionWithId(sessionId)
        store.get(sessionId) must beSome[HttpSession] and {
          (Prop.forAll(values) { kv =>
            store(sessionId) += kv
            store(sessionId)(kv._1) must_== kv._2
          }).set(minTestsOk -> 1500, workers -> 8)
        }
      } reduce (_ and _)
    }
  }

}
