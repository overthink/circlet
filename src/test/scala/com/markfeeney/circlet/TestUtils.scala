package com.markfeeney.circlet

import java.net.URI

object TestUtils {
  /**
   * Returns a minimal valid request object.  Client addr is always localhost.
   * Relative URLs are assumed to be relative to http://localhost.
   */
  def request(method: HttpMethod, uriString: String): Request = {
    val uri = new URI(uriString)
    val host = Option(uri.getHost).getOrElse("localhost")
    val port = Option(uri.getPort).filter(_ != -1).getOrElse(80)
    val scheme = Option(uri.getScheme).map(Scheme.parse).getOrElse(Scheme.Http)
    val path = uri.getRawPath
    val query = Option(uri.getRawQuery).filter(_.nonEmpty)
    Request(
      serverPort = port,
      serverName = host,
      requestMethod = method,
      remoteAddr = "localhost",
      uri = path,
      queryString = query,
      scheme = scheme
    )
  }
}
