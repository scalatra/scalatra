package org.scalatra

import org.mortbay.jetty.testing.HttpTester

class ScalatraHttpTester(t: HttpTester) {
  object header {
    def apply(k: String) = t.getHeader(k)
    def update(k: String, v: String) = t.setHeader(k,v)
  }
  def status = t.getStatus
  def body = t.getContent
}
