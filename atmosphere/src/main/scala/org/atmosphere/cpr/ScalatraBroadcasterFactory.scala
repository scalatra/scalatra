package org.atmosphere.cpr

import java.util
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import org.scalatra.atmosphere.{ ScalatraBroadcaster, WireFormat }
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.concurrent.{ Map => ConcurrentMap }

object ScalatraBroadcasterFactory {

  var broadcasterFactory: Option[BroadcasterFactory] = None
  var cfg: Option[AtmosphereConfig] = None

  def setDefault(factory: BroadcasterFactory, cfg: AtmosphereConfig) = {
    broadcasterFactory = Some(factory)
    this.cfg = Some(cfg)
  }

  def clearDefaults = {
    broadcasterFactory = None
    cfg = None
  }

  def getDefault() = broadcasterFactory

}
/**
 * As seen from class ScalatraBroadcasterFactory, the missing signatures are as follows.
 *  For convenience, these are usable as stub implementations.
 * def addBroadcasterListener(x$1: org.atmosphere.cpr.BroadcasterListener): org.atmosphere.cpr.BroadcasterFactory = ???
 * def broadcasterListeners(): java.util.Collection[org.atmosphere.cpr.BroadcasterListener] = ???
 * def removeBroadcasterListener(x$1: org.atmosphere.cpr.BroadcasterListener): org.atmosphere.cpr.BroadcasterFactory = ???
 * class ScalatraBroadcasterFactory(var cfg: AtmosphereConfig, bCfg: BroadcasterConf)(implicit wireFormat: WireFormat, system: ActorSystem) extends BroadcasterFactory {
 */
class ScalatraBroadcasterFactory(var cfg: AtmosphereConfig, bCfg: BroadcasterConf)(implicit wireFormat: WireFormat, system: ActorSystem) extends BroadcasterFactory {
  ScalatraBroadcasterFactory.setDefault(this, cfg)

  private[this] val logger = LoggerFactory.getLogger(classOf[ScalatraBroadcasterFactory])
  private[this] val store: ConcurrentMap[Any, Broadcaster] = new ConcurrentHashMap[Any, Broadcaster]().asScala

  override def configure(clazz: Class[_ <: Broadcaster], broadcasterLifeCyclePolicy: String, c: AtmosphereConfig = cfg): Unit = {
    this.cfg = c
  }

  private val broadcastListeners: java.util.Collection[org.atmosphere.cpr.BroadcasterListener] = new util.HashSet[BroadcasterListener]()

  def addBroadcasterListener(x$1: org.atmosphere.cpr.BroadcasterListener): org.atmosphere.cpr.BroadcasterFactory = {
    broadcastListeners.add(x$1)
    this
  }
  def broadcasterListeners(): java.util.Collection[org.atmosphere.cpr.BroadcasterListener] = broadcastListeners
  def removeBroadcasterListener(x$1: org.atmosphere.cpr.BroadcasterListener): org.atmosphere.cpr.BroadcasterFactory = {
    broadcastListeners.remove(x$1)
    this
  }

  private def createBroadcaster[T <: Broadcaster](c: Class[T], id: Any): T = {
    try {
      val b: T = if (classOf[ScalatraBroadcaster].isAssignableFrom(c)) {
        bCfg.broadcasterClass.getConstructor(classOf[WireFormat], classOf[ActorSystem]).newInstance(wireFormat, system).asInstanceOf[T]
      } else {
        cfg.framework().newClassInstance(c, c)
      }
      b.initialize(id.toString, bCfg.uri, cfg)
      bCfg.extraSetup(b)
      b.setSuspendPolicy(-1, Broadcaster.POLICY.FIFO)

      if (b.getBroadcasterConfig == null) {
        b.setBroadcasterConfig(new BroadcasterConfig(cfg.framework().broadcasterFilters, cfg, id.toString).init())
      }

      b.setBroadcasterLifeCyclePolicy(BroadcasterLifeCyclePolicy.NEVER)
      broadcasterListeners.asScala foreach { l =>
        b.addBroadcasterListener(l)
        l.onPostCreate(b)
      }
      b
    } catch {
      case ex: Exception => throw new BroadcasterFactory.BroadcasterCreationException(ex)
    }
  }

  def add(b: Broadcaster, id: Any): Boolean = store.put(id, b).isEmpty

  def destroy(): Unit = {
    val s = cfg.getInitParameter(ApplicationConfig.SHARED)
    if (s != null && s.equalsIgnoreCase("TRUE")) {
      logger.warn("Factory shared, will not be destroyed. That can possibly cause memory leaks if" +
        "Broadcaster where created. Make sure you destroy them manually.")
    }

    var bc: BroadcasterConfig = null
    store foreach {
      case (k, b) =>
        b.resumeAll()
        b.destroy()
        bc = b.getBroadcasterConfig
    }
    if (bc != null) bc.forceDestroy()

    store.clear()
    ScalatraBroadcasterFactory.clearDefaults
  }

  def get(): Broadcaster = lookup(UUID.randomUUID().toString)

  def get(id: Any): Broadcaster = lookup(id, createIfNull = true)

  def get[T <: Broadcaster](c: Class[T], id: Any): T = lookup(c, id)

  def lookup[T <: Broadcaster](c: Class[T], id: scala.Any): T = lookup(c, id, false)

  def lookup[T <: Broadcaster](c: Class[T], id: scala.Any, createIfNull: Boolean): T = {
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
    store.get(id) match {
      case Some(b) => b.asInstanceOf[T]
      case None => null.asInstanceOf[T]
    }
  }

  def lookup[T <: Broadcaster](id: scala.Any): T = lookup(id, createIfNull = false)

  def lookup[T <: Broadcaster](id: scala.Any, createIfNull: Boolean): T = {
    lookup(classOf[ScalatraBroadcaster], id, createIfNull).asInstanceOf[T]
  }

  def lookupAll(): java.util.Collection[Broadcaster] = {
    store.values.toList.asJavaCollection
  }

  def remove(b: Broadcaster, id: Any): Boolean = {
    val removed: Boolean = store.remove(id, b)
    if (removed) {
      logger.debug("Removing Broadcaster %s factory size now %s ".format(id, store.size))
    }
    removed
  }

  def remove(id: Any): Boolean = store.remove(id).isDefined

  def removeAllAtmosphereResource(r: AtmosphereResource): Unit = {
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
