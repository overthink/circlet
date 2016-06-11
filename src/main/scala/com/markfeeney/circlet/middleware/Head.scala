package com.markfeeney.circlet.middleware

import com.markfeeney.circlet._
import com.markfeeney.circlet.CpsConverters.middleware2Cps

/**
 * Middleware to make HEAD request handling easier. Turns HEAD requests into GET requests
 * internally and throws out the response body before returning.
 *
 * Port of https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/middleware/head.clj
 */
object Head {
  def wrap: Middleware = handler => {
    req => {
      val req0 = req.requestMethod match {
        case HttpMethod.Head => req.copy(requestMethod = HttpMethod.Get)
        case _ => req
      }
      handler(req0).map { resp =>
        if (req.requestMethod == HttpMethod.Head) {
          resp.copy(body = None)
        } else {
          resp
        }
      }
    }
  }

  def wrapCps: CpsMiddleware = wrap
}
