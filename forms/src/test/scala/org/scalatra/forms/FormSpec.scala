package org.scalatra.forms

import org.scalatest.FunSuite
import org.scalatra.i18n.Messages

class FormSpec extends FunSuite {

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

  case class Address(
    country: String
  )

  val form = mapping(
    "id" -> long(required),
    "age" -> number(required),
    "name" -> text(required, maxlength(10)),
    "email" -> trim(text(pattern(".+@.+"))),
    "remark" -> optional(text()),
    "options" -> list(text()),
    "agreement" -> boolean(),
    "address" -> mapping(
      "country" -> label("Country", text(required))
    )(Address.apply)
  )(UserInfo.apply)

  val messages = Messages()

  test("Bind parameters to form") {
    val params = Map(
      "id" -> "1",
      "age" -> "20",
      "name" -> "Scalatra",
      "email" -> " sample@scalatra.org ",
      "remark" -> "",
      "options[0]" -> "Java",
      "options[1]" -> "Scala",
      "agreement" -> "true",
      "address.country" -> "Japan"
    )

    val errors = form.validate("", params, messages)
    assert(errors.isEmpty)

    val result = form.convert("", params, messages)
    assert(result == UserInfo(
      id = 1,
      age = 20,
      name = "Scalatra",
      email = "sample@scalatra.org",
      remark = None,
      options = Seq("Java", "Scala"),
      agreement = true,
      address = Address("Japan")
    ))
  }

  test("Validation error") {
    val params = Map(
      "id" -> "NaN",
      "age" -> "",
      "name" -> "Scalatra User",
      "email" -> "test",
      "remark" -> "",
      "agreement" -> ""
    )

    val errors = form.validate("", params, messages).toMap
    assert(errors("id") == "id must be a number.")
    assert(errors("age") == "age is required.")
    assert(errors("name") == "name cannot be longer than 10 characters.")
    assert(errors("email") == "email must be '.+@.+'.")
    assert(errors("address.country") == "Country is required.")
  }
}
