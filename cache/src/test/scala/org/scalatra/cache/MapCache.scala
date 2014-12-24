package org.scalatra.cache

class MapCache extends Cache {
  var cache = new scala.collection.mutable.HashMap[String, Any]

  override def get[V](key: String): Option[V] = cache.get(key) match {
    case Some(v) => Some(v.asInstanceOf[V])
    case None => None
  }

  override def put[V](key: String, value: V): V =
    {
      cache.put(key, value.asInstanceOf[Object])
      value
    }

  override def remove(key: String): Unit = cache.remove(key)

  override def flush(): Unit = cache.clear()
}
