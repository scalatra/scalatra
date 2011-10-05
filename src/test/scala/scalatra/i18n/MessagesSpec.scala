package scalatra.i18n

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import java.util.MissingResourceException

class MessagesSpec extends WordSpec with MustMatchers with ShouldMatchers {
  val messages = new Messages
  "Messages" when {
    "able to find a message" should {
      "return some option" in {
        messages.get("name") must equal(Some("Name"))
      }
      "return the value" in {
        messages("name") must equal("Name")
      }
    }
    "unable to find a message" should {
      "return None" in {
        messages.get("missing") must equal(None)
      }
      "throw MissingResourceException" in {
        evaluating { messages("missing") } should produce[MissingResourceException]
      }
    }
  }
}
