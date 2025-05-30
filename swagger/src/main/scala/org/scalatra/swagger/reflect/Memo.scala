package org.scalatra.swagger.reflect

import java.util.concurrent.ConcurrentHashMap

private[reflect] class Memo[A, R] {
  private[this] val cache       = new ConcurrentHashMap[A, R](1500, 1, 1)
  def apply(x: A, f: A => R): R = {
    if (cache.containsKey(x))
      cache.get(x)
    else {
      val v = f(x)
      replace(x, v)
    }
  }

  def replace(x: A, v: R): R = {
    cache.put(x, v)
    v
  }
}
