package org.scalatra

import org.scalatra.util.MultiMapHeadView

class ScalatraParams(protected val multiMap: Map[String, Seq[String]]) extends MultiMapHeadView[String, String]
