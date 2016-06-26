package com.markfeeney.circlet.middleware

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import com.markfeeney.circlet.{Middleware, Request, Util}
import scala.io.Source
import scala.util.Try

/**
 * Parameters parsed from different parts of a Request.
 *
 * @param queryParams Params from the query string
 * @param formParams Params from a form post (application/x-www-form-urlencoded)
 * @param multipartParams Params from a multipart form post (multipart/form-data)
 */
case class Params(
    queryParams: Map[String, Param] = Map.empty,
    formParams: Map[String, Param] = Map.empty,
    multipartParams: Map[String, Param] = Map.empty) {

  /**
   * Merged view of all param types. Multipart params override query params which
   * override regular form params.
   */
  val all: Map[String, Param] = formParams ++ queryParams ++ multipartParams
}

object Params {

  private def isUrlEncodedForm(req: Request): Boolean = {
    req.contentType.exists(_.startsWith("application/x-www-form-urlencoded"))
  }

  private def formParams(req: Request, encoding: Charset): Map[String, StrParam] = {
    req.body match {
      case Some(is) if isUrlEncodedForm(req) =>
        val body: String = Source.fromInputStream(is, encoding.toString).mkString
        Util.formDecodeMap(body, encoding).map { case (k, v) => k -> StrParam(v) }
      case _ => Map.empty
    }
  }

  private def queryParams(req: Request, encoding: Charset): Map[String, StrParam] = {
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
        Util.formDecodeMap(qs, encoding).map { case (k, v) => k -> StrParam(v) }
      case _ => Map.empty
    }
  }

  private def addParams(req: Request, encoding: Option[Charset]): Request = {
    val cs: Charset = encoding.orElse(req.characterEncoding).getOrElse(UTF_8)
    // MultipartParams may have parsed some Params already; don't trample
    val params: Params = get(req).copy(
      queryParams = queryParams(req, cs),
      formParams = formParams(req, cs)
    )
    set(req, params)
  }

  /**
   * "Type-safe" access to parsed Params for `req`.
   *
   * @param req The request to look for Params on.
   * @return The parsed Params, if any, otherwise an empty Params instance.
   */
  // TODO: should this be Option[Params] like in (Poise's) Route.get?
  def get(req: Request): Params = {
    Try(req.attrs("params").asInstanceOf[Params]).getOrElse(Params())
  }

  /**
   * Update the params instance on req. Returns a new request.
 *
   * @param req The request to update
   * @param params The params to add to `req`
   * @return A new request with updated Params retreiveable via `Params.get()`
   */
  def set(req: Request, params: Params): Request = {
    req.updated("params", params)
  }

  /**
   * Add parsed params from the query string and request body to the request.
   * Use `Params.get(req)` to get access to them. Returns new Request.
   *
   * @param encoding The encoding to use for URL decoding. If not specified,
   *                 uses the request character encoding, or UTF-8 if no request
   *                 encoding can be found.
   */
  def mw(encoding: Option[Charset] = None): Middleware = handler => req => {
    handler(addParams(req, encoding))
  }
}
