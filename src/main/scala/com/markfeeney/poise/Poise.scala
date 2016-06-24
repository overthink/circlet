package com.markfeeney.poise

import com.markfeeney.circlet.Circlet.ifRequest
import com.markfeeney.circlet.{Handler, HttpMethod, Middleware}

object Poise {

  /**
   * Middleware that calls handler if route matches request.
   */
  def ifRoute(route: Route): Middleware = handler => req => k => {
    Route.parse(route, req.uri) match {
      case Some(routeParams) =>
        // TODO: attach routeParams to request!!!
        handler(req)(k)
      case None => k(None)
    }
  }

  private def ifMethod(method: HttpMethod) = ifRequest(_.requestMethod == method)

  /**
   * Middleware that will call your handler if the request method matches
   * `method` and the request matches route.
   */
  def ifMatches(method: HttpMethod, route: Route): Middleware = {
    ifMethod(method)(ifRoute(route))
  }

  /**
   * Return a handler that tries calling `candidate`, but falls back to `fallback` if
   * the former returns no resposne.
   */
  private def tryHandler(candidate: Handler, fallback: Handler): Handler = req => k => {
    candidate(req) {
      case resp @ Some(_) => k(resp)
      case None => fallback(req)(k)
    }
  }

  /**
   * Sequentially try each handler in `hs` until one returns a response.
   */
  def tryHandlers(hs: Handler*): Handler = hs.reduceLeft(tryHandler)

}
