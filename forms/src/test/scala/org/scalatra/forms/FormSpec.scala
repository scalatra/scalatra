package org.scalatra.forms

import org.scalatra.i18n.Messages
import org.scalatest.funsuite.AnyFunSuite

class FormSpec extends AnyFunSuite {

  case class UserInfo(
      id: Long,
      age: Int,
      name: String,
      email: String,
      remark: Option[String],
      options: Seq[String],
      agreement: Boolean,
      address: Address
  )

  case class Address(country: String)

  val form = mapping(
    "id"        -> long(required),
    "age"       -> number(required),
    "name"      -> text(required, maxlength(10)),
    "email"     -> trim(text(pattern(".+@.+"))),
    "remark"    -> optional(text()),
    "options"   -> list(text()),
    "agreement" -> boolean(),
    "address"   -> mapping("country" -> label("Country", text(required)))(Address.apply)
  )(UserInfo.apply)

  val messages = Messages()

  test("Bind parameters to form") {
    val params = Map(
      "id"              -> Seq("1"),
      "age"             -> Seq("20"),
      "name"            -> Seq("Scalatra"),
      "email"           -> Seq(" sample@scalatra.org "),
      "remark"          -> Seq(""),      // None
      "options[0]"      -> Seq("Java"),  // Indexed parameter
      "options[1]"      -> Seq("Scala"), // Indexed parameter
      "agreement"       -> Seq("true"),  // true
      "address.country" -> Seq("Japan")
    )

    val errors = form.validate("", params, messages)
    assert(errors.isEmpty)

    val result = form.convert("", params, messages)
    assert(
      result == UserInfo(
        id = 1,
        age = 20,
        name = "Scalatra",
        email = "sample@scalatra.org",
        remark = None,
        options = Seq("Java", "Scala"),
        agreement = true,
        address = Address("Japan")
      )
    )
  }

  test("Multi parameter for a list property") {
    val params = Map(
      "id"              -> Seq("1"),
      "age"             -> Seq("20"),
      "name"            -> Seq("Scalatra"),
      "email"           -> Seq(" sample@scalatra.org "),
      "remark"          -> Seq("Remark"),        // Some
      "options"         -> Seq("Java", "Scala"), // Multi parameter
      "agreement"       -> Seq("false"),         // false
      "address.country" -> Seq("Japan")
    )

    val errors = form.validate("", params, messages)
    assert(errors.isEmpty)

    val result = form.convert("", params, messages)
    assert(
      result == UserInfo(
        id = 1,
        age = 20,
        name = "Scalatra",
        email = "sample@scalatra.org",
        remark = Some("Remark"),
        options = Seq("Java", "Scala"),
        agreement = false,
        address = Address("Japan")
      )
    )
  }

  test("Validation error") {
    val params = Map(
      "id"        -> Seq("NaN"),
      "age"       -> Seq(""),
      "name"      -> Seq("Scalatra User"),
      "email"     -> Seq("test"),
      "remark"    -> Seq(""),
      "agreement" -> Seq("")
    )

    val errors = form.validate("", params, messages).toMap
    assert(errors("id") == "id must be a number.")
    assert(errors("age") == "age is required.")
    assert(errors("name") == "name cannot be longer than 10 characters.")
    assert(errors("email") == "email must be '.+@.+'.")
    assert(errors("address.country") == "Country is required.")
  }
}
