package com.markfeeney.circlet.middleware

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source
import scala.util.Try
import com.markfeeney.circlet.{StrVal, Util, Middleware, Request}

case class Params(
    queryParams: Map[String, StrVal] = Map.empty,
    formParams: Map[String, StrVal] = Map.empty) {
  /**
   * Merged set of params. Query params override form params.
   */
  lazy val all: Map[String, StrVal] = formParams ++ queryParams
}

object Params {

  private def formParams(req: Request, encoding: Charset): Map[String, StrVal] = {
    req.body match {
      case Some(is) if req.isUrlEncodedForm =>
        val body: String = Source.fromInputStream(is, encoding.toString).mkString
        Util.formDecodeMap(body, encoding)
      case _ => Map.empty
    }
  }

  private def queryParams(req: Request, encoding: Charset): Map[String, StrVal] = {
    req.queryString match {
      case Some(qs) =>
        // Note to self when I inevitibly second guess this in the future:
        // The query string component of the URL is in fact www-form-urlencoded (space encoded
        // as '+', line breaks as %0D%0A).  Other components of the URL (before the '?')
        // use a different variant (space as %20 -- never +, no special linebreak handling).
        // The grammar in RFC3986 is the best reference: https://tools.ietf.org/html/rfc3986#appendix-A
        // Interestingly, the grammar suggests "/" and ":" don't have to be percent-encoded in
        // the query string (https://tools.ietf.org/html/rfc3986#section-5.4.2), but it doesn't look
        // like even JS's encodeURIComponent honours that (but it does for the fragment !?)
        /// tl;dr - treat query string like form body, hope for best
        Util.formDecodeMap(qs, encoding)
      case _ => Map.empty
    }
  }

  private def addParams(req: Request, encoding: Option[Charset]): Request = {
    val cs: Charset = encoding.orElse(req.characterEncoding).getOrElse(UTF_8)
    // Ring ensures params are only parsed once, but I don't.  When will I learn why they do this?
    val params = Params(queryParams(req, cs), formParams(req, cs))
    req.updated("params", params)
  }

  /**
   * An empty set of Params.
   */
  val empty: Params = Params()

  /**
   * Type-safe ... access to parsed Params for `req`.
   *
   * @param req The request to look for Params on.
   * @return The parsed Params, if any, otherwise an empty Params instance.
   */
  def get(req: Request): Params = {
    Try(req.attrs("params").asInstanceOf[Params]).getOrElse(empty)
  }

  /**
   * Add parsed params from the query string and request body to the request's `attrs`
   * map under the key "params".  Returns new Request.
   *
   * @param encoding The encoding to use for URL decoding. If not specified,
   *                 uses the request character encoding, or UTF-8 if no request
   *                 encoding can be found.
   */
  def wrap(encoding: Option[Charset] = None): Middleware = handler => req => {
    handler(addParams(req, encoding))
  }
}
