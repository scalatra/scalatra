// https://github.com/harrah/xsbt/issues/257#issuecomment-2697049

import sbt._
import scala.xml._
import scala.xml.transform._

object Rewrite extends Plugin {
  def rewriter(f: PartialFunction[xml.Node, NodeSeq]): RuleTransformer = new RuleTransformer(rule(f))

  def rule(f: PartialFunction[xml.Node, NodeSeq]): RewriteRule = new RewriteRule {
    override def transform(n: xml.Node) = if (f.isDefinedAt(n)) f(n) else n
  }
}
