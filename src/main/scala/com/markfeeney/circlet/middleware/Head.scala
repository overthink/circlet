package com.markfeeney.circlet.middleware

import com.markfeeney.circlet._
import com.markfeeney.circlet.CpsConverters.middleware2Cps

/**
 * Middleware to make HEAD request handling easier. Turns HEAD requests into GET requests
 * internally and throws out the response body before returning.
 */
object Head {
  def apply: Middleware = handler => {
    req => {
      val req0 = req.requestMethod match {
        case HttpMethod.Head => req.copy(requestMethod = HttpMethod.Get)
        case _ => req
      }
      val resp = handler(req0)
      if (req.requestMethod == HttpMethod.Head) {
        resp.copy(body = None)
      } else {
        resp
      }
    }
  }

  def cps: CpsMiddleware = middleware2Cps(apply)
}
