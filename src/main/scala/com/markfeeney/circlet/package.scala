package com.markfeeney

package object circlet {
  type ResponseHeaders = Map[String, ResponseHeaderValue]
  type Handler = HttpRequest => HttpResponse
  type Middleware = Handler => Handler

  type Handler2 = (HttpRequest, HttpResponse => Done.type) => Done.type
  type Middleware2 = Handler2 => Handler2
}
