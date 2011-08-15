package org.scalatra

import specs2.ScalatraSpec

class EnvironmentFilter extends ScalatraFilter {
  get("/*/environment") {
    environment
  }

  get("/*/is-development-mode") {
    isDevelopmentMode
  }
}

class EnvironmentFilterSpec extends ScalatraSpec { def is =
  "The dev filter should"                                  ^
    "return 'development' as the environment"              ! env("dev", "development")^
    "be development mode"                                  ! isDevMode("dev", true)^
                                                           p^
  "The prod filter should"                                 ^
    "return 'development' as the environment"              ! env("prod", "production")^
    "be development mode"                                  ! isDevMode("prod", false)^
                                                           end

  val devFilterHolder = addFilter(classOf[EnvironmentFilter], "/dev/*")

  val prodFilterHolder = addFilter(classOf[EnvironmentFilter], "/prod/*")
  prodFilterHolder.setInitParameter("org.scalatra.environment", "production")

  def env(environment: String, expected: String) =
    get("/%s/environment".format(environment)) {
      body must be equalTo(expected)
    }

  def isDevMode(environment: String, expected: Boolean) =
    get("/%s/is-development-mode".format(environment)) {
      body must be equalTo(expected.toString)
    }
}
