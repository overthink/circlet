package com.markfeeney.circlet

import scala.language.implicitConversions

/**
 * Helpers to convert non-CPS handlers and middleware to their CPS bretheren.
 */
object CpsConverters {

  implicit def handler2Cps(h: Handler): CpsHandler = {
    (request, k) => k(h(request))
  }

  implicit def middleware2Cps(mw: Middleware): CpsMiddleware = {
    // could this possibly be correct?
    cpsH => (request, k) => {
      cpsH(request, response => k(mw(_ => response)(request)))
    }
  }

}
