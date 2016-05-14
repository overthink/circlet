package com.markfeeney.circlet

import java.io.InputStream
import java.nio.charset.Charset
import java.security.cert.X509Certificate

import com.markfeeney.circlet.HttpParse.reValue

import scala.util.Try

/**
 * Basic request. Modeled after Request Map in https://github.com/ring-clojure/ring/blob/master/SPEC
 *
 * @param serverPort The port on which the request is being handled.
 * @param serverName The resolved server name, or the server IP address.
 * @param remoteAddr The IP address of the client or the last proxy that sent the request.
 * @param uri The request URI, excluding the query string and the "?" separator. Must start with "/".
 * @param queryString The query string, if present.
 * @param scheme The transport protocol.
 * @param requestMethod The transport protocol.
 * @param protocol The protocol the request was made with, e.g. "HTTP/1.1"
 * @param sslClientCert The SSL client certificate, if supplied.
 * @param headers A map of lowercased header names to corresponding values.
 * @param body The request body, if present.
 * @param attrs Other info tacked on to the request.
 */
case class Request(
  serverPort: Int,
  serverName: String,
  remoteAddr: String,
  uri: String,
  queryString: Option[String] = None,
  scheme: Scheme = Scheme.Http,
  requestMethod: HttpMethod = HttpMethod.Get,
  protocol: String = "HTTP/1.1",
  sslClientCert: Option[X509Certificate] = None,
  headers: Map[String, String] = Map.empty,
  body: Option[InputStream] = None,
  attrs: Map[String, AnyRef] = Map.empty) {

  require(uri.startsWith("/"))

  /**
   * Helper for easily adding things to the attrs map.
 *
   * @param key Name of the thing to add
   * @param value The thing to add
   * @return A new HttpRequest with key/value added to attrs.
   */
  def updated(key: String, value: AnyRef): Request = {
    this.copy(attrs = attrs.updated(key, value))
  }

  /**
   * Add a response header to this Response.  Returns a new Response.
   */
  def addHeader(name: String, value: String): Request = {
    this.copy(headers = this.headers.updated(name, value))
  }

  private val CharsetRe = (";(?:.*\\s)?(?i:charset)=(" + reValue.toString + ")\\s*(?:;|$)").r

  /**
   * Get the Charset of the request, if any.  Looks in the Content-Type header.  Requests
   * may actually have a Content-Type header for POSTs and PUTs.
   */
  def characterEncoding: Option[Charset]= {
    headers.get("content-type").flatMap { ct =>
      Try(
        Charset.forName(CharsetRe.findFirstMatchIn(ct).get.group(1))
      ).toOption
      // TODO: why doesn't the code below work????
//      case CharsetRe(charset) =>
//        Try(Charset.forName(charset)).toOption
//      case _ =>
//        None
    }
  }

}
