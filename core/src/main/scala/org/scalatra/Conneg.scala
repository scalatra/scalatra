package org.scalatra

import java.nio.charset.Charset
import java.text.DecimalFormat
import javax.servlet.http.HttpServletRequest

import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

/** Represents the value of a content negotiation header. */
case class Conneg[T](value: T, q: Float = 1)

/** Defines type classes and helper methods for well known content-negotiation headers. */
object Conneg {

  // - Header parsing --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Used to parse a specific type of content negotiation header.*/
  trait Format[T] extends RegexParsers {

    def entry: Parser[Option[T]]

    val Separators: Set[Char] = {
      Set('(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' ', '\t')
    }

    // - Base elements -------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    def token: Parser[String] = """[\u0020-\u007E&&[^ \t()<>@,;:\"/\[\]?={}]]+""".r
    def quotedPair: Parser[String] = "\\" ~> """[\u0000-\u007F]""".r
    def qdtext: Parser[String] = """[\u0000-\u007f&&[^\"\\]]+""".r
    def quotedString: Parser[String] = "\"" ~> (rep(quotedPair | qdtext) ^^ (_.mkString)) <~ "\""
    def content: Parser[String] = quotedString | token
    def content(value: String): String =
      if (value.exists(mustEscape)) "\"%s\"" format value.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")
      else value

    private def mustEscape(c: Char): Boolean = {
      c < '\u0020' || c > '\u007E' || Separators.contains(c)
    }

    // - Parameters ----------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    def valueSep: Parser[Any] = """\s*=\s*""".r
    def paramSep: Parser[Any] = """\s*;\s*""".r

    def parameter: Parser[(String, String)] = (token ~ (valueSep ~> (token | quotedString))) ^^ {
      case token ~ value => (token, value)
    }

    def parameters: Parser[Map[String, String]] = repsep(parameter, paramSep) ^^ {
      _.foldLeft(Map[String, String]()) {
        case (params, param) => params + param
      }
    }

    // - Conneg specific -----------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    private val qFormat: DecimalFormat = new DecimalFormat("0.###")

    /** Parser for a single conneg value. */
    def conneg: Parser[Option[Conneg[T]]] = entry ~ qValue ^^ {
      case Some(entry) ~ q => Some(new Conneg(entry, q))
      case _ => None
    }

    /** Parser for a list of conneg values. */
    def connegs: Parser[List[Option[Conneg[T]]]] = repsep(conneg, ",")

    /** Parser for the content-negotiation `q` parameter. */
    def qValue: Parser[Float] = {
      opt(paramSep ~> ("q" ~ valueSep) ~> """[0-1](\.[0-9]{1,3})?""".r ^^ (_.toFloat)) ^^ {
        case Some(q) => q
        case _ => 1.0f
      }
    }

    def values(raw: String): List[Conneg[T]] = {
      parseAll(connegs, raw) match {
        case Success(a, _) => a.collect { case Some(v) => v }
        case _ => List()
      }
    }
  }

  // - Value retrieval -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /**
   * Retrieves all supported values of the specified content-negotiation header.
   *
   * Note that any value declared in the header but not supported by the system will be absent from the list. For
   * example, an `Accept-Charset` value of `utf-256` will yield an empty list.
   *
   * Additionally, this method swallows errors silently. An invalid header value will yield an empty list rather than
   * an exception.
   */
  def values[T](name: String)(implicit req: HttpServletRequest, format: Format[T]): List[Conneg[T]] = {
    val header = req.getHeader(name)
    if (header == null) List()
    else format.values(header.trim())
  }

  /** Retrieves the preferred supported value for the specified content-negotiation header. */
  def preferredValue[T](name: String)(implicit req: HttpServletRequest, format: Format[T]): Option[T] = {
    val all = values(name)

    if (all.isEmpty) None
    else Some(all.reduce { (a, b) => if (a.q < b.q) b else a }.value)
  }

  // - Encoding --------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val AcceptEncoding: String = "Accept-Encoding"

  implicit object EncodingFormat extends Format[ContentEncoding] {
    override def entry: EncodingFormat.Parser[Option[ContentEncoding]] = token ^^ ContentEncoding.forName
  }

  def preferredEncoding(implicit req: HttpServletRequest): Option[ContentEncoding] = preferredValue(AcceptEncoding)
  def acceptedEncodings(implicit req: HttpServletRequest): List[Conneg[ContentEncoding]] = values(AcceptEncoding)

  // - Charset ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val AcceptCharset: String = "Accept-Charset"

  implicit object CharsetFormat extends Format[Charset] {
    override def entry = token ^^ { s => Try(Charset.forName(s)).toOption }
  }

  def preferredCharset(implicit req: HttpServletRequest): Option[Charset] = preferredValue[Charset](AcceptCharset)
  def acceptedCharsets(implicit req: HttpServletRequest): List[Conneg[Charset]] = values(AcceptCharset)
}
