package org.scalatra

import java.net.URL
import scala.collection.mutable

trait ApplicationContext extends mutable.Map[String, AnyRef] {
  def mount(service: Service, urlPattern: String, name: String): Unit

  def mount(service: Service, urlPattern: String): Unit =
    mount(service, urlPattern, service.getClass.getName)

  def contextPath: String

  def initParameters: mutable.Map[String, String]

  def resource(path: String): Option[URL]
}
