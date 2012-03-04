package org.scalatra

/**
 * Trait representing an object that can't be fully initialized by its
 * constructor.  Useful for unifying the initialization process of an
 * HttpServlet and a Filter.
 */
trait Initializable { 
  type Context
  type Config 

  trait RichConfig {
    def context: Context
    def initParameters: Map[String, String]
  }
  protected implicit def configWrapper(config: Config): RichConfig

  /**
   * A hook to initialize the class with some configuration after it has
   * been constructed.
   *
   * Not called init because GenericServlet doesn't override it, and then
   * we get into https://lampsvn.epfl.ch/trac/scala/ticket/2497.
   */
  def initialize(config: Config)
}
