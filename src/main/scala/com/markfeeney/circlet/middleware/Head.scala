package com.markfeeney.circlet.middleware

import com.markfeeney.circlet._

/**
 * Middleware to make HEAD request handling easier. Changes request into a GET, processes
 * as usual, then throws away the body before returning.
 */
object Head {
  def apply: CpsMiddleware = cpsH =>
    (req, k) => {
      println(s"Head middleware: unmolested request $req")
      val req0: Request =
        req.requestMethod match {
          case HttpMethod.Head => req.copy(requestMethod = HttpMethod.Get)
          case _ => req
        }
      cpsH(req0, resp => {
        println(s"Throwing away old body: '${resp.body}'")
        k(resp.copy(body = None))
      })
    }
}
