package org

package object scalatra 
  extends Control // make halt and pass visible to helpers outside the DSL
//  with DefaultValues // make defaults visible
{
  import util.MultiMap

  type RouteTransformer = (Route => Route)

  type MultiParams = MultiMap

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"
  
  @deprecated("Use org.scalatra.servlet.ServletBase if you depend on the Servlet API, or org.scalatra.ScalatraBase if you don't.", "2.1.0")
  type ScalatraKernel = servlet.ServletBase
  
//  class OptionDefaults[T](value: Option[T]) {
//	  /**
//	   * Returns the item contained in the Option if it is defined, otherwise, the default element for the type A
//	   * <p/>
//	   * For example:
//	   * <pre>
//	   * val o: Option = None
//	   * val a: List[String] = ~o
//	   * </pre>
//	   */
//	  def unary_~(implicit z: DefaultValue[T]): T = value getOrElse z.default
//	
//	  def orDefault(implicit z: DefaultValue[T]): T = value getOrElse z.default
//	}
//  implicit def option2optionWithDefault[T](opt: Option[T]) = new OptionDefaults(opt)
}
