package org.scalatra
package swagger

import org.scalatra.json.JacksonJsonSupport

trait JacksonSwaggerBase extends ScalatraBase with JacksonJsonSupport with CorsSupport with SwaggerBase
