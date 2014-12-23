package org.scalatra.scalate

import java.io.File

import org.fusesource.scalate.TemplateEngine

class ScalatraTemplateEngine(sourceDirectories: Traversable[File] = None, mode: String = sys.props.getOrElse("scalate.mode", "production")) extends TemplateEngine(sourceDirectories, mode) {

}