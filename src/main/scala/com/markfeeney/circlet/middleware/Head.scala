package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.Circlet.modifyResponse
import com.markfeeney.circlet.{CpsHandler, HttpMethod}

/**
 * Middleware to make HEAD request handling easier. Turns HEAD requests into GET requests
 * internally and throws out the response body before returning.
 *
 * Port of https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/middleware/head.clj
 */
object Head {

  def mw(handler: CpsHandler): CpsHandler = req => {
    val req0 = req.requestMethod match {
      case HttpMethod.Head => req.copy(requestMethod = HttpMethod.Get)
      case _ => req
    }
    val mw = modifyResponse { resp =>
      if (req.requestMethod == HttpMethod.Head) {
        resp.copy(body = None)
      } else {
        resp
      }
    }
    mw(handler)(req0)
  }

}
