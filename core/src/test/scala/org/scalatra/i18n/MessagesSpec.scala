package org.scalatra
package i18n

import org.scalatest.{ Matchers, WordSpec }
import java.util.MissingResourceException

class MessagesSpec extends WordSpec with Matchers {
  val messages = Messages()
  "Messages" when {
    "able to find a message" should {
      "return some option" in {
        messages.get("name") should equal(Some("Name"))
      }
      "return the value" in {
        messages("name") should equal("Name")
      }
    }
    "unable to find a message" should {
      "return None" in {
        messages.get("missing") should equal(None)
      }
      "throw MissingResourceException" in {
        an[MissingResourceException] should be thrownBy {
          messages("missing")
        }
      }
    }
  }
}
