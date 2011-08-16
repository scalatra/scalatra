package org.scalatra

trait UrlGenerator {

  def generate(route: Route, params: Pair[String, String]*): String =
    generate(route, params.toMap)

  def generate(route: Route, params: Map[String, String] = Map()): String =
    route.reversibleMatcher match {
      case Some(matcher: SinatraRouteMatcher) =>
        SinatraUrlGenerator.generate(matcher.path, params)
      case Some(matcher) =>
        throw new Exception("Cannot generate from \"%s\"" format (matcher))
      case None =>
        throw new Exception("Route \"%s\" is not reversible" format (route))
    }

  object SinatraUrlGenerator {

    def generate(path: String, params: Map[String, String]): String = {
      replaceNamed(path, params)
    }

    private def replaceNamed(path: String, params: Map[String, String]) =
      """:[^/?#]+""".r replaceAllIn (path, s =>
        params.get(s.toString.tail) match {
          case Some(value) => value
          case None => throw new Exception("The url \"%s\" requires param \"%s\"" format (path, s))
        })
  }
}
