package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.Future

// TODO rename file and class
// https://github.com/scalatra/scalatra/pull/1410
// https://github.com/akka/akka/pull/31561
// https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka
class AkkaSupportAfterFilterFilter extends ScalatraFilter with FutureSupport {
  var actionTime: Long = _
  var afterTime: Long = _
  var afterCount: Long = _
  protected override implicit val executor = scala.concurrent.ExecutionContext.global

  asyncGet("/async") {
    Thread.sleep(2000)
    actionTime = System.nanoTime()

    "async"
  }

  asyncGet("/async-two-afters") {
    Thread.sleep(2000)
    "async-two-afters"
  }

  get("/sync") {
    "sync"
  }

  get("/future") {
    Future({
      "future"
    })
  }

  after() {
    afterCount += 1
    afterTime = System.nanoTime()
  }

  after("/async-two-afters") {
    afterCount += 1
    afterTime = System.nanoTime()
  }

  def reset(): Unit = {
    actionTime = 0L
    afterTime = 0L
    afterCount = 0L
  }

  override def destroy(): Unit = {
    super.destroy()
  }
}

class AkkaSupportAfterFilterForFiltersSpec extends MutableScalatraSpec {
  sequential

  val filter = new AkkaSupportAfterFilterFilter()
  addFilter(filter, "/foo/*")
  addFilter(filter, "/*")

  "after filters for asynchronous actions" should {
    "run after the action" in {
      filter.reset()

      get("/async") {
        filter.afterTime must beGreaterThan(filter.actionTime)
      }
    }

    "run only once" in {
      filter.reset()

      get("/async") {
        filter.afterCount mustEqual 1
      }
    }

    "all execute" in {
      filter.reset()

      get("/async-two-afters") {
        filter.afterCount mustEqual 2
      }
    }

    "work when contextPath != /" in {
      pending("Until we come up with a thread-safe solution to this")
      filter.reset()

      get("/foo/async-two-afters") {
        filter.afterCount mustEqual 2
      }
    }
  }

  "after filters for synchronous get with Future return value" should {
    "run after the action" in {
      filter.reset()

      get("/future") {
        filter.afterTime must beGreaterThan(filter.actionTime)
      }
    }

    "run only once" in {
      filter.reset()

      get("/future") {
        filter.afterCount mustEqual 1
      }
    }
  }

  "after filters for synchronous actions" should {
    "run normally" in {
      filter.reset()

      get("/sync") {
        filter.afterCount mustEqual 1
      }
    }
  }
}
