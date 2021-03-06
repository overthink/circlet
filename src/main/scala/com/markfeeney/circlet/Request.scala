package com.markfeeney.circlet

import java.io.InputStream
import java.net.URI
import java.nio.charset.{Charset, StandardCharsets}
import java.security.cert.X509Certificate
import java.util.{Locale, Scanner}

import com.markfeeney.circlet.HttpParse.value

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

  require(!new URI(uri).isAbsolute, "Absolute URLs not supported")

  /**
   * Helper to convert body into a String. Beware using it on huge bodies.
   */
  def bodyString(encoding: Charset = StandardCharsets.UTF_8): Option[String] = {
    this.body.flatMap { is =>
      Cleanly(new Scanner(is, encoding.toString).useDelimiter("\\A"))(_.close) { s =>
        if (s.hasNext()) {
          s.next()
        } else {
          ""
        }
      }.right.toOption
    }
  }

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
   * Helper to add a header to this Request.  Returns a new Request containing a header with name/value.
   * NB: the header name is converted to lowercase before being stored.
   *
   * @param name Name of the header to add, e.g. "Content-type".
   * @param value Value of the header, e.g. "text/html"
   */
  def addHeader(name: String, value: String): Request = {
    this.copy(headers = this.headers.updated(name.toLowerCase(Locale.ENGLISH), value))
  }

  /**
   * Helper to get a header from the request.
   */
  def getHeader(name: String): Option[String] = {
    headers.get(name.toLowerCase)
  }

  private val CharsetRe = (";(?:.*\\s)?(?i:charset)=(" + value.toString + ")\\s*(?:;|$)").r

  /**
   * Get the Charset of the request, if any.  Looks in the Content-Type header.  Requests
   * may actually have a Content-Type header for POSTs and PUTs.
   */
  def characterEncoding: Option[Charset]= {
    // exercise for reader: the code below, but pattern matching CharsetRe -- seems impossible?
    // due to ... non-capturing groups?
    for {
      ct <- headers.get("content-type")
      m <- CharsetRe.findFirstMatchIn(ct) if m.groupCount >= 1
      charset <- Try(Charset.forName(m.group(1))).toOption
    } yield charset
  }

  /** Helper to get content type since it is so commonly used. */
  def contentType: Option[String] = {
    headers.get("content-type")
  }

  /** Helper to set content type since it is so commonly used.  Returns new Request. */
  def setContentType(value: String): Request = {
    addHeader("content-type", value)
  }

  /** Helper to get content length since it is so commonly used. */
  def contentLength: Option[Long] = {
    headers.get("content-length").flatMap(cl => Try(cl.toLong).toOption)
  }

  /** Helper to set content length since it is so commonly used.  Returns new Request. */
  def setContentLength(value: Long): Request = {
    addHeader("content-length", value.toString)
  }

}

object Request {

  /**
   * A minimal `Request` useful for simple tests.  Will be moved to a test lib in the future.
   */
  // TODO: get this out of public API
  def mock(method: HttpMethod, url: String): Request = {
    Request(
      serverPort = 80,
      serverName = "example.com",
      remoteAddr = "localhost",
      uri = url,
      requestMethod = method
    )
  }

  def mock(url: String): Request = mock(HttpMethod.Get, url)

}