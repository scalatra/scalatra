package org.scalatra

import akka.event.Logging
import grizzled.slf4j.Logger

trait ScalatraLogging {
  implicit def appContext: AppContext

  protected lazy val logger = Logger(getClass)

}

