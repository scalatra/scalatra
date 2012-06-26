package org.scalatra

/**
 * Trait representing an object that can't be fully initialized by its
 * constructor.  Useful for unifying the initialization process of an
 * HttpServlet and a Filter.
 */
trait Initializable { 


  /**
   * A hook to initialize the class with some configuration after it has
   * been constructed.
   */
  def initialize(config: AppContext)
}
