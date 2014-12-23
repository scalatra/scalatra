package org.scalatra

import org.scalatra.util.{ MapWithIndifferentAccess, MultiMapHeadView }

class ScalatraParams(
  protected val multiMap: Map[String, Seq[String]])
    extends MultiMapHeadView[String, String]
    with MapWithIndifferentAccess[String]
