package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
 * An `Handler` is the Scalatra abstraction for an object that operates on a request/response pair.
 *
 * Such an object can, for example, look at the request headers and deny access to a resource, or transform
 * request parameters and later call the `handle()` method of the class in which it had been mixed in, or anything else.
 *
 * `Handler`s are somewhat similar to servlet Filters, but since they are traits that can be mixed in they also
 * provide a form of code reuse. For example, [[org.scalatra.FlashMapSupport]] and [[org.scalatra.CookieSupport]] are two
 * `Handler`s that provide utility methods to access the request/response pair.
 *
 * [[org.scalatra.ScalatraKernel]] (and thus all the Scalatra applications) is a `Handler` too: its `handle()` method works
 * as a dispatcher to the possible routes. Thanks to trait-stacking, any same-named method that is mixed in in your Scalatra
 * servlet or filter gets called together with the dispatching action, providing the mixed in functionalities to all the invoked actions.
 *
 * `Handler` instances are usually shared among threads thus any state change produced in this method (outsid of side effects on the arguments to `handle(req,res)` should be wrapped in a [[scala.util.DynamicVariable]] , which is thread-local.
 */
trait Handler {
  /**
   * Handles a request and writes to the response.
   */
  def handle(req: HttpServletRequest, res: HttpServletResponse): Unit
}

