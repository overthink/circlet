package com.markfeeney.circlet

import scala.language.implicitConversions

/**
 * Helpers to convert non-CPS handlers and middleware to their CPS bretheren.
 */
object CpsConverters {

  implicit def handler2Cps(h: Handler): CpsHandler = {
    (request, k) => k(h(request))
  }

  // TBD if this should be publicly available
  private def cps2Handler(cpsH: CpsHandler): Handler = {
    var resp: Response = null // yes really, much evil here
    request => {
      cpsH(request, response => {
        resp = response
        Done
      })
      resp
    }
  }

  implicit def middleware2Cps(mw: Middleware): CpsMiddleware = {
    cpsH => mw(cps2Handler(cpsH))
  }

}
