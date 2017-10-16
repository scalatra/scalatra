package org.scalatra.spring

import java.lang.reflect.Constructor

import grizzled.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter
import org.springframework.stereotype.Component

/** @author Stephen Samuel */
@Component
@deprecated("Spring integration has been no longer maintained. It will be dropped in 2.7.0.", "2.6.0")
class ConstructorAutowiredAnnotationBeanPostProcessor
  extends InstantiationAwareBeanPostProcessorAdapter
  with org.springframework.core.Ordered
  with Logging {

  logger.error("Configured for Scala constructor support")

  override def determineCandidateConstructors(beanClass: Class[_], beanName: String): Array[Constructor[_]] = {
    if (isAutowiredClass(beanClass) && beanClass.getDeclaredConstructors.size == 1) {
      Array(beanClass.getDeclaredConstructors()(0))
    } else {
      null
    }
  }

  def isAutowiredClass(beanClass: Class[_]) = beanClass.isAnnotationPresent(classOf[Autowired])

  def getOrder: Int = Integer.MAX_VALUE
}