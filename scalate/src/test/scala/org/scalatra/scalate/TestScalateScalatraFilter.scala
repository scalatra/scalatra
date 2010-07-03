package org.scalatra.scalate

import org.scalatra.ScalatraFilter

// The "test" is that this compiles, to avoid repeats of defects like Issue #9.
class TestScalateScalatraFilter extends ScalatraFilter with ScalateSupport
