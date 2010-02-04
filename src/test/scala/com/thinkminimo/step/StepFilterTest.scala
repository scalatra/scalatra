package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

/*
 * There are four types of servlet mappings: path, extension, default, and exact-match.  Test them all, as they can all
 * cause different splits of pathInfo vs. servletPath.
 */

class StepFilterTestFilter extends StepFilter {
  get("/path-mapped/filtered") {
    "filter"
  }

  get("/filtered.do") {
    "filter"
  }

  get("/filtered") {
    "filter"
  }

  get("/exact-match/filtered") {
    "filter"
  }
}

class StepFilterTestPathMappedServlet extends Step {
  get("/filtered") {
    "path-mapped"
  }

  get("/unfiltered") {
    "path-mapped"
  }
}

class StepFilterTestExtensionMappedServlet extends Step {
  get("/filtered.do") {
    "extension-mapped"
  }

  get("/unfiltered.do") {
    "extension-mapped"
  }
}

class StepFilterTestDefaultServlet extends Step {
  get("/filtered") {
    "default"
  }

  get("/unfiltered") {
    "default"
  }
}

class StepFilterTestExactMatchServlet extends Step {
  get("/exact-match/filtered") {
    "exact match"
  }

  get("/exact-match/unfiltered") {
    "exact match"
  }
}

class StepFilterTest extends StepSuite with ShouldMatchers {
  routeFilter(classOf[StepFilterTestFilter], "/*")

  // See SRV.11.2 of Servlet 2.5 spec for the gory details of servlet mappings
  route(classOf[StepFilterTestPathMappedServlet], "/path-mapped/*")
  route(classOf[StepFilterTestExtensionMappedServlet], "*.do")
  route(classOf[StepFilterTestDefaultServlet], "/")
  route(classOf[StepFilterTestExactMatchServlet], "/exact-match/filtered")
  route(classOf[StepFilterTestExactMatchServlet], "/exact-match/unfiltered")

  test("should filter matching request to path-mapped servlet") {
    get("/path-mapped/filtered") {
      body should equal("filter")
    }
  }

  test("should pass through unmatched request to path-mapped servlet") {
    get("/path-mapped/unfiltered") {
      body should equal("path-mapped")
    }
  }

  test("should filter matching request to extension-mapped servlet") {
    get("/filtered.do") {
      body should equal("filter")
    }
  }

  test("should pass through unmatched request to extension-mapped servlet") {
    get("/unfiltered.do") {
      body should equal("extension-mapped")
    }
  }

  test("should filter matching request to default servlet") {
    get("/filtered") {
      body should equal("filter")
    }
  }

  test("should pass through unmatched request to default servlet") {
    get("/unfiltered") {
      body should equal("default")
    }
  }

  test("should filter matching request to exact-match-mapped servlet") {
    get("/exact-match/filtered") {
      body should equal("filter")
    }
  }

  test("should pass through unmatched request to exact-match-mapped servlet") {
    get("/exact-match/unfiltered") {
      body should equal("exact match")
    }
  }
}
