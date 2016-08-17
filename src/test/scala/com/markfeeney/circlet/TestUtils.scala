package com.markfeeney.circlet

import java.net.{ServerSocket, URI}

import org.eclipse.jetty.server.Server

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

  /**
   * App (handler) that returns a "Hello World" body and X-Foo response header for all requests.
   */
  val hwApp: Handler = Circlet.handler { req =>
    req.requestMethod match {
      case HttpMethod.Get =>
        // Add a header so we can be sure this code executed even if body is remmoved
        Response(body = "Hello world", headers = Map("X-Foo" -> Vector("42")))
      case _ =>
        Response(status = 404, body = "not found")
    }
  }

  private def findFreePort: Int = {
    Cleanly(new ServerSocket(0))(_.close)(_.getLocalPort).right.get
  }

  case class TestJettyServer(server: Server, opts: JettyOptions)

  // disable logging from Jetty
  org.eclipse.jetty.util.log.Log.setLog(new NoJettyLogging)

  /**
   * Bootstrap a server using given options and handler, and run f on said
   * server, ensuring we clean up.
   */
  def testServer(h: Handler, opts: JettyOptions)(f: TestJettyServer => Unit): Unit = {
    val opts0 = opts.copy(
      join = false,
      httpPort = findFreePort,
      sslPort = if (opts.allowSsl) findFreePort else opts.sslPort
    )
    val result = Cleanly(JettyAdapter.run(h, opts0))(_.stop()) { server =>
      f(TestJettyServer(server, opts0))
    }
    result.left.foreach(e => throw e)
  }

}
