package com.markfeeney.circlet

import scala.language.implicitConversions

/**
 * Helpers to convert non-CPS handlers and middleware to their CPS bretheren.
 */
object NonCps {

  implicit def handler2Cps(h: Handler): CpsHandler = {
    (request, cont) => cont(h(request))
  }

  implicit def middleware2Cps(mw: Middleware): CpsMiddleware = {
    // could this possibly be correct?
    (cpsH) => {
      (request, cont) => {
        cpsH(request, response => cont(mw(_ => response)(request)))
      }
    }
  }

}
