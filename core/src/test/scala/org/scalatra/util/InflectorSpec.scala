package org.scalatra.util

import org.specs2.Specification
import org.specs2.matcher.DataTables
import org.specs2.execute.Result

class InflectorSpec extends Specification with DataTables {
  def is =
    "The inflector should" ^
      "dasherize a string" ! dasherization ^
      "humanize a string" ! humanization ^
      "ordinalize a string" ! stringOrdinalization ^
      "ordinalize an int" ! intOrdinalization ^
      "pascalize a string" ! pascalization ^
      "camelize a string" ! camelization ^
      "titleize a string" ! titleization ^
      "uncapitalize a string" ! uncapitalization ^
      "underscore a string" ! underscoring ^
      "pluralize a string" ! pluralization ^
      "singularize a string" ! singularization ^ end

  def dasherization = {
    "source" || "target" |
      "some_title" !! "some-title" |
      "some-title" !! "some-title" |
      "some_title_goes_here" !! "some-title-goes-here" |
      "some_title_and_another" !! "some-title-and-another" |> { (src, tgt) =>
        Inflector.dasherize(src) must_== tgt
      }
  }

  def humanization = {
    "source" || "target" |
      "some_title" !! "Some title" |
      "some-title" !! "Some-title" |
      "someTitle" !! "Sometitle" |
      "someTitle_Another" !! "Sometitle another" |> { (src, tgt) =>
        Inflector.humanize(src) must_== tgt
      }
  }

  def stringOrdinalization = {
    "source" || "target" |
      "0" !! "0th" |
      "1" !! "1st" |
      "2" !! "2nd" |
      "3" !! "3rd" |
      "4" !! "4th" |
      "5" !! "5th" |
      "6" !! "6th" |
      "7" !! "7th" |
      "8" !! "8th" |
      "9" !! "9th" |
      "10" !! "10th" |
      "11" !! "11th" |
      "12" !! "12th" |
      "13" !! "13th" |
      "14" !! "14th" |
      "20" !! "20th" |
      "21" !! "21st" |
      "22" !! "22nd" |
      "23" !! "23rd" |
      "24" !! "24th" |
      "100" !! "100th" |
      "101" !! "101st" |
      "102" !! "102nd" |
      "103" !! "103rd" |
      "104" !! "104th" |
      "110" !! "110th" |
      "1000" !! "1000th" |
      "1001" !! "1001st" |> { (src, tgt) =>
        Inflector.ordinalize(src) must_== tgt
      }
  }

  def intOrdinalization = {
    "source" || "target" |
      0 !! "0th" |
      1 !! "1st" |
      2 !! "2nd" |
      3 !! "3rd" |
      4 !! "4th" |
      5 !! "5th" |
      6 !! "6th" |
      7 !! "7th" |
      8 !! "8th" |
      9 !! "9th" |
      10 !! "10th" |
      11 !! "11th" |
      12 !! "12th" |
      13 !! "13th" |
      14 !! "14th" |
      20 !! "20th" |
      21 !! "21st" |
      22 !! "22nd" |
      23 !! "23rd" |
      24 !! "24th" |
      100 !! "100th" |
      101 !! "101st" |
      102 !! "102nd" |
      103 !! "103rd" |
      104 !! "104th" |
      110 !! "110th" |
      1000 !! "1000th" |
      1001 !! "1001st" |> { (src, tgt) =>
        Inflector.ordinalize(src) must_== tgt
      }
  }

  def pascalization = {
    "source" || "target" |
      "customer" !! "Customer" |
      "CUSTOMER" !! "CUSTOMER" |
      "CUStomer" !! "CUStomer" |
      "customer_name" !! "CustomerName" |
      "customer_first_name" !! "CustomerFirstName" |
      "customer_first_name_goes_here" !! "CustomerFirstNameGoesHere" |
      "customer name" !! "Customer name" |> { (src, tgt) =>
        Inflector.pascalize(src) must_== tgt
      }
  }

  def camelization = {
    "source" || "target" |
      "customer" !! "customer" |
      "CUSTOMER" !! "cUSTOMER" |
      "CUStomer" !! "cUStomer" |
      "customer_name" !! "customerName" |
      "customer_first_name" !! "customerFirstName" |
      "customer_first_name_goes_here" !! "customerFirstNameGoesHere" |
      "customer name" !! "customer name" |> { (src, tgt) =>
        Inflector.camelize(src) must_== tgt
      }
  }

  def titleization = {
    "source" || "target" |
      "some title" !! "Some Title" |
      "some title" !! "Some Title" |
      "sometitle" !! "Sometitle" |
      "some-title: The begining" !! "Some Title: The Begining" |
      "some_title:_the_begining" !! "Some Title: The Begining" |
      "some title: The_begining" !! "Some Title: The Begining" |> { (src, tgt) =>
        Inflector.titleize(src) must_== tgt
      }
  }

  def uncapitalization = {
    "source" || "target" |
      "some title" !! "some title" |
      "some Title" !! "some Title" |
      "SOMETITLE" !! "sOMETITLE" |
      "someTitle" !! "someTitle" |
      "some title goes here" !! "some title goes here" |
      "some TITLE" !! "some TITLE" |> { (src, tgt) =>
        Inflector.uncapitalize(src) must_== tgt
      }
  }

  def underscoring = {
    "source" || "target" |
      "SomeTitle" !! "some_title" |
      "some title" !! "some_title" |
      "some title that will be underscored" !! "some_title_that_will_be_underscored" |
      "SomeTitleThatWillBeUnderscored" !! "some_title_that_will_be_underscored" |> { (src, tgt) =>
        Inflector.underscore(src) must_== tgt
      }
  }

  def pluralization   = pluralAndSingular { (left, right) => Inflector.pluralize(left) must_== right }
  def singularization = pluralAndSingular { (left, right) => Inflector.singularize(right) must_== left }

  def pluralAndSingular(execFn: (String, String) => Result) = {
    "singular" || "plural" |
      "search" !! "searches" |
      "switch" !! "switches" |
      "fix" !! "fixes" |
      "box" !! "boxes" |
      "process" !! "processes" |
      "address" !! "addresses" |
      "case" !! "cases" |
      "stack" !! "stacks" |
      "wish" !! "wishes" |
      "fish" !! "fish" |
      "category" !! "categories" |
      "query" !! "queries" |
      "ability" !! "abilities" |
      "agency" !! "agencies" |
      "movie" !! "movies" |
      "archive" !! "archives" |
      "index" !! "indices" |
      "wife" !! "wives" |
      "safe" !! "saves" |
      "half" !! "halves" |
      "move" !! "moves" |
      "salesperson" !! "salespeople" |
      "person" !! "people" |
      "spokesman" !! "spokesmen" |
      "man" !! "men" |
      "woman" !! "women" |
      "basis" !! "bases" |
      "diagnosis" !! "diagnoses" |
      "datum" !! "data" |
      "medium" !! "media" |
      "analysis" !! "analyses" |
      "node_child" !! "node_children" |
      "child" !! "children" |
      "experience" !! "experiences" |
      "day" !! "days" |
      "comment" !! "comments" |
      "foobar" !! "foobars" |
      "newsletter" !! "newsletters" |
      "old_news" !! "old_news" |
      "news" !! "news" |
      "series" !! "series" |
      "species" !! "species" |
      "quiz" !! "quizzes" |
      "perspective" !! "perspectives" |
      "ox" !! "oxen" |
      "photo" !! "photos" |
      "buffalo" !! "buffaloes" |
      "tomato" !! "tomatoes" |
      "dwarf" !! "dwarves" |
      "elf" !! "elves" |
      "information" !! "information" |
      "equipment" !! "equipment" |
      "bus" !! "buses" |
      "status" !! "statuses" |
      "status_code" !! "status_codes" |
      "mouse" !! "mice" |
      "louse" !! "lice" |
      "house" !! "houses" |
      "octopus" !! "octopi" |
      "virus" !! "viri" |
      "alias" !! "aliases" |
      "portfolio" !! "portfolios" |
      "vertex" !! "vertices" |
      "matrix" !! "matrices" |
      "axis" !! "axes" |
      "testis" !! "testes" |
      "crisis" !! "crises" |
      "rice" !! "rice" |
      "shoe" !! "shoes" |
      "horse" !! "horses" |
      "prize" !! "prizes" |
      "edge" !! "edges" |
      "goose" !! "geese" |
      "deer" !! "deer" |
      "sheep" !! "sheep" |
      "wolf" !! "wolves" |
      "volcano" !! "volcanoes" |
      "aircraft" !! "aircraft" |
      "alumna" !! "alumnae" |
      "alumnus" !! "alumni" |
      "fungus" !! "fungi" |> { execFn }
  }

}
