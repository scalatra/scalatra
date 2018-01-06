package org.scalatra

import _root_.akka.actor.ActorSystem
import org.atmosphere.cpr.AtmosphereResource

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration._
import scala.util.control.Exception._

package object atmosphere {

  type AtmoReceive = PartialFunction[InboundMessage, Unit]

  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
  val AtmosphereRouteKey = "org.scalatra.atmosphere.AtmosphereRoute"
  val ActorSystemKey = "org.scalatra.atmosphere.ActorSystem"
  val TrackMessageSize = "org.scalatra.atmosphere.TrackMessageSize"

  import org.scalatra.servlet.ServletApiImplicits._

  implicit def atmoResourceWithClient(res: AtmosphereResource) = new {
    def clientOption = res.session.get(AtmosphereClientKey).asInstanceOf[Option[AtmosphereClient]]
    def client = res.session.apply(AtmosphereClientKey).asInstanceOf[AtmosphereClient]
  }

  private[atmosphere] implicit def jucFuture2akkaFuture[T](javaFuture: java.util.concurrent.Future[T])(implicit system: ActorSystem): Future[T] = {
    implicit val execContext = system.dispatcher
    val promise = Promise[T]()
    pollJavaFutureUntilDoneOrCancelled(javaFuture, promise)
    promise.future
  }

  // See here: http://stackoverflow.com/questions/11529145/how-do-i-wrap-a-java-util-concurrent-future-in-an-akka-future
  private[atmosphere] def pollJavaFutureUntilDoneOrCancelled[T](javaFuture: java.util.concurrent.Future[T], promise: Promise[T], maybeTimeout: Option[Deadline] = None)(implicit system: ActorSystem): Unit = {
    implicit val execContext = system.dispatcher
    if (maybeTimeout.exists(_.isOverdue())) javaFuture.cancel(true)

    if (javaFuture.isDone || javaFuture.isCancelled) {
      promise.complete(allCatch withTry { javaFuture.get }).future
    } else {
      system.scheduler.scheduleOnce(10 milliseconds) {
        pollJavaFutureUntilDoneOrCancelled(javaFuture, promise, maybeTimeout)
      }
    }
  }

  //  private[atmoshpere] val atmoScheduler = Executors.newScheduledThreadPool(1)
}
