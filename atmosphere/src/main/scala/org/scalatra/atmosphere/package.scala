package org.scalatra

import _root_.akka.dispatch.Future
import _root_.akka.dispatch.Promise
import _root_.akka.util.Deadline
import _root_.akka.util.duration._
import _root_.akka.actor.ActorSystem
import org.atmosphere.cpr.AtmosphereResource
import scala.util.control.Exception._

package object atmosphere {

  type AtmoReceive = PartialFunction[InboundMessage, Unit]
  type ClientFilter = AtmosphereClient => Boolean

  import org.scalatra.servlet.ServletApiImplicits._

  implicit def atmoResourceWithClient(res: AtmosphereResource) = new {
    def clientOption = res.session.get(ScalatraAtmosphereHandler.AtmosphereClientKey).asInstanceOf[Option[AtmosphereClient]]
    def client = res.session.apply(ScalatraAtmosphereHandler.AtmosphereClientKey).asInstanceOf[AtmosphereClient]
  }

  private[atmosphere] implicit def jucFuture2akkaFuture[T](javaFuture: java.util.concurrent.Future[T])(implicit system: ActorSystem): Future[T] = {
    implicit val execContext = system.dispatcher
    val promise = Promise[T]()
    pollJavaFutureUntilDoneOrCancelled(javaFuture, promise)
    promise
  }

  // See here: http://stackoverflow.com/questions/11529145/how-do-i-wrap-a-java-util-concurrent-future-in-an-akka-future
  private[atmosphere] def pollJavaFutureUntilDoneOrCancelled[T](javaFuture: java.util.concurrent.Future[T], promise: Promise[T], maybeTimeout: Option[Deadline] = None)(implicit system: ActorSystem) {
    implicit val execContext = system.dispatcher
    if (maybeTimeout.exists(_.isOverdue())) javaFuture.cancel(true)

    if (javaFuture.isDone || javaFuture.isCancelled) {
      promise.complete(allCatch either { javaFuture.get })
    } else {
      system.scheduler.scheduleOnce(10 milliseconds) {
        pollJavaFutureUntilDoneOrCancelled(javaFuture, promise, maybeTimeout)
      }
    }
  }
}
