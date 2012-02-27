package org.scalatra

import java.net.URL
import scala.collection.mutable

trait ApplicationContext extends mutable.Map[String, AnyRef] {
  def mount(service: Service, urlPattern: String): Unit

  def contextPath: String

  def initParameters: mutable.Map[String, String]

  def resource(path: String): Option[URL]
}
