package org.scalatra.util

import java.util.Locale.ENGLISH
import annotation.tailrec
import scala.util.matching.Regex

trait Inflector {

  def titleize(word: String): String =
    """\b([a-z])""".r.replaceAllIn(
      humanize(underscore(word)),
      _.group(0).toUpperCase(ENGLISH)
    )
  def humanize(word: String): String = capitalize(word.replace("_", " "))
  def camelize(word: String): String = {
    val w = pascalize(word)
    w.substring(0, 1).toLowerCase(ENGLISH) + w.substring(1)
  }
  def pascalize(word: String): String = {
    val lst = word.split("_").toList
    (lst.headOption
      .map(s => s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1))
      .get ::
      lst.tail.map(s => s.substring(0, 1).toUpperCase + s.substring(1)))
      .mkString("")
  }
  def underscore(word: String): String = {
    val spacesPattern = "[-\\s]".r
    val firstPattern = "([A-Z]+)([A-Z][a-z])".r
    val secondPattern = "([a-z\\d])([A-Z])".r
    val replacementPattern = "$1_$2"
    spacesPattern
      .replaceAllIn(
        secondPattern.replaceAllIn(
          firstPattern.replaceAllIn(word, replacementPattern),
          replacementPattern
        ),
        "_"
      )
      .toLowerCase
  }

  def capitalize(word: String): String =
    word.substring(0, 1).toUpperCase(ENGLISH) + word
      .substring(1)
      .toLowerCase(ENGLISH)
  def uncapitalize(word: String): String =
    word.substring(0, 1).toLowerCase(ENGLISH) + word.substring(1)
  def ordinalize(word: String): String = ordanize(word.toInt, word)
  def ordinalize(number: Int): String = ordanize(number, number.toString)
  private def ordanize(number: Int, numberString: String) = {
    val nMod100 = number % 100
    if (nMod100 >= 11 && nMod100 <= 13) numberString + "th"
    else {
      (number % 10) match {
        case 1 => numberString + "st"
        case 2 => numberString + "nd"
        case 3 => numberString + "rd"
        case _ => numberString + "th"
      }
    }
  }
  def dasherize(word: String): String = underscore(word).replace('_', '-')
  def pluralize(word: String): String = applyRules(plurals, word)
  def singularize(word: String): String = applyRules(singulars, word)

  private object Rule {
    def apply(kv: (String, String)) = new Rule(kv._1, kv._2)
  }
  private class Rule(pattern: String, replacement: String) {

    private val regex = ("""(?i)%s""" format pattern).r

    def apply(word: String) =
      if (regex.findFirstIn(word).isEmpty) {
        None
      } else {
        val m = regex.replaceAllIn(word, replacement)
        if (m == null || m.trim.isEmpty) None
        else Some(m)
      }
  }
  private implicit def tuple2Rule(pair: (String, String)): Rule = Rule(pair)

  @tailrec
  private def applyRules(collection: List[Rule], word: String): String =
    if (uncountables.contains(word.toLowerCase(ENGLISH))) word
    else {
      if (collection.isEmpty) return word
      val m = collection.head(word)
      if (m.isDefined)
        m.get // using getOrElse doesn't allow for @tailrec optimization
      else applyRules(collection.tail, word)
    }

  private var plurals = List[Rule]()

  private var singulars = List[Rule]()
  private var uncountables = List[String]()

  def addPlural(pattern: String, replacement: String): Unit =
    plurals ::= pattern -> replacement
  def addSingular(pattern: String, replacement: String): Unit =
    singulars ::= pattern -> replacement
  def addIrregular(singular: String, plural: String): Unit = {
    plurals ::= (("(" + singular(0) + ")" + singular.substring(
      1
    ) + "$") -> ("$1" + plural.substring(1)))
    singulars ::= (("(" + plural(0) + ")" + plural.substring(
      1
    ) + "$") -> ("$1" + singular.substring(1)))
  }
  def addUncountable(word: String) = uncountables ::= word

  def interpolate(text: String, vars: Map[String, String]) =
    """\#\{([^}]+)\}""".r.replaceAllIn(
      text,
      (_: Regex.Match) match {
        case Regex.Groups(v) => vars.getOrElse(v, "")
      }
    )

}

trait InflectorImports {

  implicit def string2InflectorString(word: String): Inflector.InflectorString =
    new Inflector.InflectorString(word)
  implicit def int2InflectorInt(number: Int): Inflector.InflectorInt =
    new Inflector.InflectorInt(number)

}

object InflectorImports extends InflectorImports

object Inflector extends Inflector {

  class InflectorString(private val word: String) extends AnyVal {
    def titleize = Inflector.titleize(word)
    def humanize = Inflector.humanize(word)
    def camelize = Inflector.camelize(word)
    def pascalize = Inflector.pascalize(word)
    def underscore = Inflector.underscore(word)
    def dasherize = Inflector.dasherize(word)
    def uncapitalize = Inflector.uncapitalize(word)
    def ordinalize = Inflector.ordinalize(word)
    def pluralize = Inflector.pluralize(word)
    def singularize = Inflector.singularize(word)
    def fill(values: (String, String)*) =
      Inflector.interpolate(word, Map(values*))
  }

  class InflectorInt(private val number: Int) extends AnyVal {
    def ordinalize = Inflector.ordinalize(number)
  }

  addPlural("$", "s")
  addPlural("s$", "s")
  addPlural("(ax|test)is$", "$1es")
  addPlural("(octop|vir|alumn|fung)us$", "$1i")
  addPlural("(alias|status)$", "$1es")
  addPlural("(bu)s$", "$1ses")
  addPlural("(buffal|tomat|volcan)o$", "$1oes")
  addPlural("([ti])um$", "$1a")
  addPlural("sis$", "ses")
  addPlural("(?:([^f])fe|([lr])f)$", "$1$2ves")
  addPlural("(hive)$", "$1s")
  addPlural("([^aeiouy]|qu)y$", "$1ies")
  addPlural("(x|ch|ss|sh)$", "$1es")
  addPlural("(matr|vert|ind)ix|ex$", "$1ices")
  addPlural("([m|l])ouse$", "$1ice")
  addPlural("^(ox)$", "$1en")
  addPlural("(quiz)$", "$1zes")

  addSingular("s$", "")
  addSingular("(n)ews$", "$1ews")
  addSingular("([ti])a$", "$1um")
  addSingular(
    "((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$",
    "$1$2sis"
  )
  addSingular("(^analy)ses$", "$1sis")
  addSingular("([^f])ves$", "$1fe")
  addSingular("(hive)s$", "$1")
  addSingular("(tive)s$", "$1")
  addSingular("([lr])ves$", "$1f")
  addSingular("([^aeiouy]|qu)ies$", "$1y")
  addSingular("(s)eries$", "$1eries")
  addSingular("(m)ovies$", "$1ovie")
  addSingular("(x|ch|ss|sh)es$", "$1")
  addSingular("([m|l])ice$", "$1ouse")
  addSingular("(bus)es$", "$1")
  addSingular("(o)es$", "$1")
  addSingular("(shoe)s$", "$1")
  addSingular("(cris|ax|test)es$", "$1is")
  addSingular("(octop|vir|alumn|fung)i$", "$1us")
  addSingular("(alias|status)es$", "$1")
  addSingular("^(ox)en", "$1")
  addSingular("(vert|ind)ices$", "$1ex")
  addSingular("(matr)ices$", "$1ix")
  addSingular("(quiz)zes$", "$1")

  addIrregular("person", "people")
  addIrregular("man", "men")
  addIrregular("child", "children")
  addIrregular("sex", "sexes")
  addIrregular("move", "moves")
  addIrregular("goose", "geese")
  addIrregular("alumna", "alumnae")

  addUncountable("equipment")
  addUncountable("information")
  addUncountable("rice")
  addUncountable("money")
  addUncountable("species")
  addUncountable("series")
  addUncountable("fish")
  addUncountable("sheep")
  addUncountable("deer")
  addUncountable("aircraft")
}
