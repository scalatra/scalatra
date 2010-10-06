package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class EnvironmentFilter extends ScalatraFilter {
  get("/*/environment") {
    environment
  }

  get("/*/is-development-mode") {
    isDevelopmentMode
  }
}

class EnvironmentFilterTest extends ScalatraFunSuite with ShouldMatchers {
  val devFilterHolder = addFilter(classOf[EnvironmentFilter], "/dev/*")

  val prodFilterHolder = addFilter(classOf[EnvironmentFilter], "/prod/*")
  prodFilterHolder.setInitParameter("org.scalatra.environment", "production")

  test("default environment is 'development'") {
    get("/dev/environment") {
      body should equal ("development")
    }
  }

  test("is development mode if environment is 'development'") {
    get("/dev/is-development-mode") {
      body should equal ("true")
    }
  }

  test("environment comes from org.scalatra.environment init parameter") {
    get("/prod/environment") {
      body should equal ("production")
    }
  }

  test("is not development mode if environment is 'production'") {
    get("/prod/is-development-mode") {
      body should equal ("false")
    }
  }
}