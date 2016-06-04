package com.markfeeney.circlet

import scala.language.implicitConversions

/**
 * Helpers to convert non-CPS handlers and middleware to their CPS bretheren.
 */
object CpsConverters {

  implicit def handler2Cps(h: Handler): CpsHandler = {
    (request, k) => k(h(request))
  }

  implicit def cps2Handler(cpsH: CpsHandler): Handler = {
    var resp: Response = null // yes really, much evil here
    request => {
      cpsH(request, response => {
        resp = response
        Sent
      })
      resp
    }
  }

  implicit def cps2Middleware(cpsMw: CpsMiddleware): Middleware = {
    handler => cps2Handler(cpsMw(handler2Cps(handler)))
  }

  implicit def middleware2Cps(mw: Middleware): CpsMiddleware = {
    cpsH => handler2Cps(mw(cps2Handler(cpsH)))
  }

}
