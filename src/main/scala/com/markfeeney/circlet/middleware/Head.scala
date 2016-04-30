package com.markfeeney.circlet.middleware

import com.markfeeney.circlet._

/**
 * Middleware to make HEAD request handling easier. Changes request into a GET, processes
 * as usual, then throws away the body before returning.
 */
object Head {
  //def apply(h: Handler2): Handler2 = {
  def apply: CpsMiddleware = { (h: CpsHandler) =>
    (req: HttpRequest, cont: HttpResponse => Done.type) => {
      println(s"Head middleware: unmolested request $req")
      val req0: HttpRequest =
        req.requestMethod match {
          case HttpMethod.Head => req.copy(requestMethod = HttpMethod.Get)
          case _ => req
        }
      val cont0: HttpResponse => Done.type = { resp =>
        println(s"Throwing away old body: '${resp.body}'")
        cont(resp.copy(body = None))
      }
      h(req0, cont0)
    }
  }
}
