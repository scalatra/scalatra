// https://github.com/harrah/xsbt/issues/257#issuecomment-2697049

import scala.xml._
import scala.xml.transform._

object Rewrite {
  def rewriter(f: PartialFunction[Node, NodeSeq]): RuleTransformer = new RuleTransformer(rule(f))

  def rule(f: PartialFunction[Node, NodeSeq]): RewriteRule = new RewriteRule {
    override def transform(n: Node) = if (f.isDefinedAt(n)) f(n) else n
  }
}
