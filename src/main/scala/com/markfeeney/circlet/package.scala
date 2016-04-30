package com.markfeeney

package object circlet {
  type ResponseHeaders = Map[String, ResponseHeaderValue]

  type CpsHandler = (HttpRequest, HttpResponse => Done.type) => Done.type
  type CpsMiddleware = CpsHandler => CpsHandler

  type Handler = HttpRequest => HttpResponse
  type Middleware = Handler => Handler
}
