package org.scalatra

import javax.servlet.http.HttpServletRequest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraFunSuite

/*
 * There are four types of servlet mappings: path, extension, default, and exact-match.  Test them all, as they can all
 * cause different splits of pathInfo vs. servletPath.
 */

class ScalatraFilterTestFilter extends ScalatraFilter {
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

  get("/status-202") {
    status = 202
  }

  get("/init-param/:name") {
    initParameter(params("name")).toString
  }

  get("/encoded-uri/:name") {
    params("name")
  }

  get("/encoded-uri-2/中国话不用彁字。") {
    "中国话不用彁字。"
  }
}

class ScalatraFilterTestPathMappedServlet extends ScalatraServlet {
  get("/filtered") {
    "path-mapped"
  }

  get("/unfiltered") {
    "path-mapped"
  }
}

class ScalatraFilterTestExtensionMappedServlet extends ScalatraServlet {
  get("/filtered.do") {
    "extension-mapped"
  }

  get("/unfiltered.do") {
    "extension-mapped"
  }

  // Non path-mapped servlets need this to work
  override def requestPath(implicit request: HttpServletRequest) = request.getServletPath
}

class ScalatraFilterTestDefaultServlet extends ScalatraServlet {
  get("/filtered") {
    "default"
  }

  get("/unfiltered") {
    "default"
  }

  // Non path-mapped servlets need this to work
  override def requestPath(implicit request: HttpServletRequest) = request.getServletPath
}

class ScalatraFilterTestExactMatchServlet extends ScalatraServlet {
  get("/exact-match/filtered") {
    "exact match"
  }

  get("/exact-match/unfiltered") {
    "exact match"
  }

  // Non path-mapped servlets need this to work
  override def requestPath(implicit request: HttpServletRequest) = request.getServletPath
}

@RunWith(classOf[JUnitRunner])
class ScalatraFilterTest extends ScalatraFunSuite {
  val filterHolder = addFilter(classOf[ScalatraFilterTestFilter], "/*")
  filterHolder.setInitParameter("cat-who-is-biting-me", "Pete")

  // See SRV.11.2 of Servlet 2.5 spec for the gory details of servlet mappings
  override def skipDefaultServlet = true
  addServlet(classOf[ScalatraFilterTestPathMappedServlet], "/path-mapped/*")
  addServlet(classOf[ScalatraFilterTestExtensionMappedServlet], "*.do")
  addServlet(classOf[ScalatraFilterTestExactMatchServlet], "/exact-match/filtered")
  addServlet(classOf[ScalatraFilterTestExactMatchServlet], "/exact-match/unfiltered")
  addServlet(classOf[ScalatraFilterTestDefaultServlet], "/")

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

  test("init parameter returns Some if set") {
    get("/init-param/cat-who-is-biting-me") {
      body should equal("Some(Pete)")
    }
  }

  test("init parameter returns None if not set") {
    get("/init-param/derp") {
      body should equal("None")
    }
  }

  test("handles encoded characters in uri") {
    get("/encoded-uri/ac/dc") {
      status should equal(404)
    }

    get("/encoded-uri/ac%2Fdc") {
      status should equal(200)
      body should equal("ac/dc")
    }

    get("/encoded-uri/%23toc") {
      status should equal(200)
      body should equal("#toc")
    }

    get("/encoded-uri/%3Fquery") {
      status should equal(200)
      body should equal("?query")
    }

    get("/encoded-uri/Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%2F%3F%23") {
      status should equal(200)
      body should equal("Fußgängerübergänge/?#")
    }

    get("/encoded-uri-2/中国话不用彁字。") {
      status should equal(200)
    }

    get("/encoded-uri-2/%E4%B8%AD%E5%9B%BD%E8%AF%9D%E4%B8%8D%E7%94%A8%E5%BD%81%E5%AD%97%E3%80%82") {
      status should equal(200)
    }

    // mixing encoded with decoded characters
    get("/encoded-uri-2/中国%E8%AF%9D%E4%B8%8D%E7%94%A8%E5%BD%81%E5%AD%97%E3%80%82") {
      status should equal(200)
    }
  }
}

class ScalatraFilterWithoutServletMappingTest extends ScalatraFunSuite {
  addFilter(classOf[ScalatraFilterTestFilter], "/*")

  // Based on http://gist.github.com/519565, http://gist.github.com/519566.
  // Was instead giving a 404.
  test("should match even when no servlet is mapped") {
    get("/status-202") {
      status should equal(202)
    }
  }
}

