package org.atmosphere.cpr

import org.json4s.Formats
import akka.actor.ActorSystem
import grizzled.slf4j.Logger
import collection.mutable
import java.util.concurrent.ConcurrentHashMap
import org.atmosphere.di.InjectorProvider
import java.util.UUID
import org.scalatra.atmosphere.{WireFormat, ScalatraBroadcaster}
import collection.JavaConverters._

object ScalatraBroadcasterFactory {

}
class ScalatraBroadcasterFactory(cfg: AtmosphereConfig)(implicit wireFormat: WireFormat, system: ActorSystem) extends BroadcasterFactory {
  BroadcasterFactory.setBroadcasterFactory(this, cfg)

  private[this] val logger = Logger[ScalatraBroadcasterFactory]
  private[this] val store: mutable.ConcurrentMap[Any, Broadcaster] = new ConcurrentHashMap[Any, Broadcaster]().asScala

  private def createBroadcaster(c: Class[_<:Broadcaster], id: Any): Broadcaster = {
    try {
      val b: Broadcaster = if (classOf[ScalatraBroadcaster].isAssignableFrom(c)) {
        new ScalatraBroadcaster(id.toString, cfg)
      } else {
        c.getConstructor(classOf[String], classOf[AtmosphereConfig]).newInstance(id.toString, cfg)

      }
      InjectorProvider.getInjector.inject(b)

      if (b.getBroadcasterConfig == null) {
        b.setBroadcasterConfig(new BroadcasterConfig(cfg.framework().broadcasterFilters, cfg, "scalatra-broadcaster-factory"))
      }

      b.setBroadcasterLifeCyclePolicy(BroadcasterLifeCyclePolicy.NEVER)
      broadcasterListeners.asScala foreach { l =>
        b.addBroadcasterListener(l)
        l.onPostCreate(b)
      }
      b
    } catch {
      case ex: Exception => throw new DefaultBroadcasterFactory.BroadcasterCreationException(ex)
    }
  }
  def add(b: Broadcaster, id: Any): Boolean = store.put(id, b).isEmpty

  def destroy() {
    val s = cfg.getInitParameter(ApplicationConfig.SHARED)
    if (s != null && s.equalsIgnoreCase("TRUE")) {
        logger.warn("Factory shared, will not be destroyed. That can possibly cause memory leaks if" +
                "Broadcaster where created. Make sure you destroy them manually.")
    }

    var bc: BroadcasterConfig = null
    store foreach { case (k, b) =>
      b.resumeAll()
      b.destroy()
      bc = b.getBroadcasterConfig
    }
    if (bc != null) bc.forceDestroy()

    store.clear()
    BroadcasterFactory.factory = null
  }

  def get(): Broadcaster = lookup(UUID.randomUUID().toString)

  def get(c: Class[_ <: Broadcaster], id: Any): Broadcaster = lookup(c, id)

  def get(id: Any): Broadcaster = lookup(id, createIfNull = true)

  def lookup(c: Class[_ <: Broadcaster], id: Any): Broadcaster = lookup(c, id, false)

  def lookup(c: Class[_ <: Broadcaster], id: Any, createIfNull: Boolean): Broadcaster = {
    val bOpt = store get id
    if (bOpt.isDefined && !c.isAssignableFrom(bOpt.get.getClass)) {
      val msg = "Invalid lookup class " + c.getName + ". Cached class is: " + bOpt.get.getClass.getName
      logger.warn(msg)
      throw new IllegalStateException(msg)
    }

    if ((bOpt.isEmpty && createIfNull) || (bOpt.isDefined && bOpt.get.isDestroyed)) {
      if (bOpt.isDefined) {
        val b = bOpt.get
        logger.debug("Removing destroyed Broadcaster %s" format b.getID)
        store.remove(b.getID, b)
      }
      if (store.putIfAbsent(id, createBroadcaster(c, id)) == null) {
        logger.debug("Added Broadcaster %s. Factory size: %s.".format(id, store.size))
      }

    }
    store.get(id).orNull
  }

  def lookup(id: Any): Broadcaster = lookup(id, createIfNull = false)

  def lookup(id: Any, createIfNull: Boolean): Broadcaster = lookup(classOf[ScalatraBroadcaster], id, createIfNull)

  def lookupAll(): java.util.Collection[Broadcaster] = {
    store.values.toList.asJavaCollection
  }

  def remove(b: Broadcaster, id: Any): Boolean = {
    val removed: Boolean = store.remove(id, b)
     if (removed) {
       logger.debug("Removing Broadcaster {} factory size now {} ", id, store.size)
     }
     removed
  }

  def remove(id: Any): Boolean = store.remove(id).isDefined

  def removeAllAtmosphereResource(r: AtmosphereResource) {
    // Remove inside all Broadcaster as well.
    try {
      if (store.nonEmpty) {
        try {
          store.valuesIterator foreach { b =>
            if (b.getAtmosphereResources.contains(r))
              b.removeAtmosphereResource(r)
          }
        } catch {
          case ex: IllegalStateException => logger.debug(ex.getMessage, ex)
        }
      }
    } catch {
      case ex: Exception => {
        logger.warn(ex.getMessage, ex)
      }
    }
  }
}
