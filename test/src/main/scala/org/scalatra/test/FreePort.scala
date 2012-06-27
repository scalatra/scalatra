package org.scalatra.test

import java.net.ServerSocket

object FreePort {
  def apply(): Int = {
    val s = new ServerSocket(0)
    try { s.getLocalPort } finally { s.close() }
  }
}
