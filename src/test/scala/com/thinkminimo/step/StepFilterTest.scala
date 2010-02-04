package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class StepFilterTestFilter extends StepFilter {
  get("/path-mapped/filtered") {
    "filter"
  }
}

class StepFilterTestPathMappedServlet extends Step {
  get("/filtered")  {
    "servlet"
  }

  get("/unfiltered") {
    "servlet"
  }
}

class StepFilterTest extends StepSuite with ShouldMatchers {
  routeFilter(classOf[StepFilterTestFilter], "/*")
  route(classOf[StepFilterTestPathMappedServlet], "/path-mapped/*")

  test("should filter matching request to path-mapped servlet") {
    get("/path-mapped/filtered") {
      body should equal ("filter")
    }
  }

  test("should pass through unmatched request to path-mapped servlet") {
    get("/path-mapped/unfiltered") {
      body should equal ("servlet")
    }
  }
}
