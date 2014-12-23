package org.scalatra
package i18n

import org.scalatra.test.scalatest.ScalatraWordSpec

class I18nSupportServlet extends ScalatraServlet with I18nSupport {
  get("/:key") {
    val msgs = messages
    msgs(params("key"))
  }

  get("/getcookie") {
    cookies.get(I18nSupport.LocaleKey) match {
      case Some(v) => v
      case _ => "None"
    }
  }

  post("/setcookie") {
    cookies.update(I18nSupport.LocaleKey, params(I18nSupport.LocaleKey))
  }
}

class I18nSupportSpec extends ScalatraWordSpec {
  addServlet(classOf[I18nSupportServlet], "/*")

  "Servlet with I18nSupport" should {
    "handle locale change via HTTP param and set it to cookie" in {
      session {
        get("/name", I18nSupport.LocaleKey -> "id_ID") { // Bug in Java6: id_ID is changed to in_ID by java.util.Locale
          body should equal("Nama")
        }
        get("/getcookie") {
          body should equal("id_ID")
        }
        get("/age") {
          body should equal("Umur")
        }
      }
    }
    "handle locale change via Accept-Language header as the last option" in {
      session {
        get("/name", Map.empty[String, String], Map("Accept-Language" -> "en-AU,de;q=0.8,en-US;q=0.6,en;q=0.4")) {
          body should equal("Name en_AU")
        }
        get("/getcookie") {
          body should equal("None")
        }
      }
    }
    "prefer locale change via HTTP param over Accept-Language header" in {
      session {
        get("/name", Map(I18nSupport.LocaleKey -> "id_ID"), Map("Accept-Language" -> "en-AU,de;q=0.8,en-US;q=0.6,en;q=0.4")) {
          body should equal("Nama")
        }
        get("/getcookie") {
          body should equal("id_ID")
        }
        get("/age") {
          body should equal("Umur")
        }
      }
    }
    "update locale cookie with new locale from HTTP param" in {
      session {
        // Set the initial cookie
        post("/setcookie", I18nSupport.LocaleKey -> "en_US") {}
        get("/getcookie") {
          body should equal("en_US")
        }
        get("/name") {
          body should equal("Name")
        }

        // Set the locale in HTTP param
        get("/name", Map(I18nSupport.LocaleKey -> "id_ID"), Map("Accept-Language" -> "en-AU,de;q=0.8,en-US;q=0.6,en;q=0.4")) {
          body should equal("Nama")
        }
        get("/getcookie") {
          body should equal("id_ID")
        }
        get("/age") {
          body should equal("Umur")
        }
      }
    }
    "fallback unknown locale to default locale" in {
      get("/name", Map(I18nSupport.LocaleKey -> "xyz")) {
        body should equal("Name")
      }
    }
  }
}
