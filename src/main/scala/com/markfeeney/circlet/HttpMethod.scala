package com.markfeeney.circlet

import java.util.Locale

sealed trait HttpMethod
object HttpMethod {
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Head extends HttpMethod
  case object Put extends HttpMethod
  case object Options extends HttpMethod
  case object Delete extends HttpMethod
  case object Trace extends HttpMethod
  case object Connect extends HttpMethod
  case object Move extends HttpMethod
  case object Proxy extends HttpMethod
  case object Pri extends HttpMethod

  def parse(s: String): HttpMethod = {
    s.toLowerCase(Locale.ENGLISH) match {
      case "get" => Get
      case "post" => Post
      case "head" => Head
      case "put" => Put
      case "options" => Options
      case "delete" => Delete
      case "trace" => Trace
      case "connect" => Connect
      case "move" => Move
      case "proxy" => Proxy
      case "pri" => Pri
      case other =>
        sys.error(s"Could not parse '$other' to HttpMethod")
    }
  }
}
