package com.thinkminimo.step

import org.mortbay.jetty.testing.HttpTester

class StepHttpTester(t: HttpTester) {
  object header {
    def apply(k: String) = t.getHeader(k)
    def update(k: String, v: String) = t.setHeader(k,v)
  }
  def status = t.getStatus
  def body = t.getContent
}
