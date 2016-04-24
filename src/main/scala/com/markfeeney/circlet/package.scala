package com.markfeeney

package object circlet {
  type ResponseHeaders = Map[String, ResponseHeaderValue]
  type Handler = HttpRequest => HttpResponse
  type Middleware = Handler => Handler
}
