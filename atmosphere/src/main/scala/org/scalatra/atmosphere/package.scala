package org.scalatra

import _root_.akka.dispatch.Future
import _root_.akka.dispatch.Promise
import _root_.akka.util.Deadline
import _root_.akka.util.duration._
import _root_.akka.actor.ActorSystem
import org.atmosphere.cpr.{AtmosphereResourceFactory, ApplicationConfig, AtmosphereResource}
import scala.util.control.Exception._
import java.util.concurrent.{ConcurrentHashMap, Executors}
import collection.mutable
import collection.JavaConverters._
import annotation.switch

package object atmosphere {

  type AtmoReceive = PartialFunction[InboundMessage, Unit]
  type ClientFilter = AtmosphereClient => Boolean

  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
  val AtmosphereRouteKey = "org.scalatra.atmosphere.AtmosphereRoute"
  val ActorSystemKey = "org.scalatra.atmosphere.ActorSystem"
  val TrackMessageSize = "org.scalatra.atmosphere.TrackMessageSize"

  private[atmosphere] val activeClients: mutable.ConcurrentMap[String, AtmosphereClient] = new ConcurrentHashMap[String, AtmosphereClient].asScala

  implicit def atmoResourceWithClient(res: AtmosphereResource) = new {

    def realUuid = {
      res.transport match {
        case AtmosphereResource.TRANSPORT.WEBSOCKET =>
          val uuidOpt = Option(res.getRequest.getAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID))
          uuidOpt map (_.asInstanceOf[String]) getOrElse res.uuid
        case _ => res.uuid
      }
    }
    def clientOption = activeClients.get(realUuid) map { client =>
      client.resource = AtmosphereResourceFactory.getDefault.find(realUuid)
      client
    }
    def client = {
      val cl = activeClients(realUuid)
      cl.resource = AtmosphereResourceFactory.getDefault.find(realUuid)
      cl
    }
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

//  private[atmosphere] val atmoScheduler = Executors.newSingleThreadScheduledExecutor()
}
