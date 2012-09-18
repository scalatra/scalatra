package org.scalatra
package atmosphere

import _root_.akka.actor._
import _root_.akka.dispatch._
import _root_.akka.util.Deadline
import _root_.akka.util.duration._
import collection.JavaConverters._
import grizzled.slf4j.Logger
import org.atmosphere.cpr._
import org.json4s.Formats
import scala.util.control.Exception.allCatch


class ScalatraBroadcaster(id: String, config: AtmosphereConfig)(implicit formats: Formats, system: ActorSystem) extends DefaultBroadcaster(id, config) {

  private[this] implicit val execContext =  system.dispatcher

  private[this] val logger: Logger = Logger[ScalatraBroadcaster]

  def broadcast[T <: OutboundMessage](msg: T, clientFilter: ClientFilter): Future[T] = {
    val wireFormat: WireFormat = new SimpleJsonWireFormat
    val selectedResources = resources.asScala filter clientFilter
    logger.debug("Sending %s to %s".format(msg, selectedResources.map(_.uuid())))
    broadcast(wireFormat.render(msg), selectedResources.toSet.asJava).map(_ => msg)
  }

  implicit def jucFuture2akkaFuture[T](javaFuture: java.util.concurrent.Future[T]): Future[T] = {
    val promise = Promise[T]()
    pollJavaFutureUntilDoneOrCancelled(javaFuture, promise)
    promise
  }

  // See here: http://stackoverflow.com/questions/11529145/how-do-i-wrap-a-java-util-concurrent-future-in-an-akka-future
  def pollJavaFutureUntilDoneOrCancelled[T](javaFuture: java.util.concurrent.Future[T], promise: Promise[T], maybeTimeout: Option[Deadline] = None) {
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