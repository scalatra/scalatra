package org.scalatra
package swagger

import json.JacksonJsonSupport

trait JacksonSwaggerBase extends ScalatraSyntax with JacksonJsonSupport with CorsSupport with SwaggerBase
