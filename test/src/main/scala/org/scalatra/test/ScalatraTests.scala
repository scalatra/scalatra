package org.scalatra.test

/**
 * Provides a framework-agnostic way to test your Scalatra app.  You probably want to extend this with
 * either <code>org.scalatra.test.scalatest.ScalatraSuite</code> or
 * <code>org.scalatra.test.specs.ScalatraSpecification</code>.
 *
 * Cookies are crudely supported within session blocks.  No attempt is made
 * to match domains, paths, or max-ages; the request sends a Cookie header
 * to match whatever Set-Cookie call it received on the previous response.
 */
trait ScalatraTests extends EmbeddedJettyContainer with HttpComponentsClient {}

