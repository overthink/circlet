package com.markfeeney.circlet.middleware

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import com.markfeeney.circlet.{Middleware, Request}

case class Params(
    queryParams: Map[String, String] = Map.empty,
    formParams: Map[String, String] = Map.empty) {
  /**
   * Merged set of params. Form params get precedence over query string params.
   */
  lazy val params: Map[String, String] = queryParams ++ formParams
}

object Params {

  private def formParams(req: Request, encoding: Charset): Map[String, String] = ???

  private def queryParams(req: Request, encoding: Charset): Map[String, String] = ???

  private def addParams(req: Request, encoding: Option[Charset] = None): Request = {
    val cs: Charset = encoding.orElse(req.characterEncoding).getOrElse(UTF_8)
    // Ring ensures params are only parsed once, but I don't.  When will I learn why they do this?
    val params = Params(queryParams(req, cs), formParams(req, cs))
    req.updated("params", params)
  }

  /**
   * Add parsed params from the query string and request body to the request's `attrs`
   * map under the key "params".  Returns new Request.
   *
   * @param encoding The encoding to use for URL decoding. If not specified,
   *                 uses the request character encoding, or UTF-8 if no request
   *                 encoding can be found.
   */
  def wrap(encoding: Option[Charset]): Middleware = handler => req => {
    handler(addParams(req, encoding))
  }
}
