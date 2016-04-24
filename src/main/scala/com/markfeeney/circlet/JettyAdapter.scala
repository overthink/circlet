package com.markfeeney.circlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.markfeeney.circlet.JettyOptions.ClientAuth.{Want, Need}
import com.markfeeney.circlet.JettyOptions.SslStoreConfig.{Instance, Path}
import org.eclipse.jetty.server._
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ThreadPool}

/**
 * Functionality for running handlers on Jetty.
 */
object JettyAdapter {

  private def createThreadPool(opts: JettyOptions): ThreadPool = {
    val pool = new QueuedThreadPool(opts.maxThreads)
    pool.setMinThreads(opts.minThreads)
    pool.setDaemon(opts.daemonThreads)
    pool
  }

  private def httpConfig(opts: JettyOptions): HttpConfiguration = {
    val c = new HttpConfiguration()
    c.setSendDateHeader(opts.sendDateHeader)
    c.setOutputBufferSize(opts.outputBufferSize)
    c.setRequestHeaderSize(opts.requestHeaderSize)
    c.setResponseHeaderSize(opts.responseHeaderSize)
    c.setSendServerVersion(opts.sendServerVersion)
    c
  }

  private def serverConnector(
      server: Server,
      factories: Seq[ConnectionFactory]): ServerConnector = {
    new ServerConnector(server, factories:_*)
  }

  private def httpConnector(server: Server, opts: JettyOptions): ServerConnector = {
    val factory = new HttpConnectionFactory(httpConfig(opts))
    val connector = serverConnector(server, Seq(factory))
    connector.setPort(opts.httpPort)
    opts.host.foreach(connector.setHost)
    connector.setIdleTimeout(opts.maxIdleTime)
    connector
  }

  private def sslContextFactory(opts: JettyOptions): SslContextFactory = {
    val context = new SslContextFactory

    opts.keyStore.foreach {
      case Path(path) => context.setKeyStorePath(path)
      case Instance(keyStore) => context.setKeyStore(keyStore)
    }
    opts.keyStorePassword.foreach(context.setKeyStorePassword)

    opts.trustStore.foreach {
      case Path(path) => context.setTrustStorePath(path)
      case Instance(keyStore) => context.setTrustStore(keyStore)
    }
    opts.trustStorePassword.foreach(context.setTrustStorePassword)

    opts.clientAuth.foreach {
      case Need => context.setNeedClientAuth(true)
      case Want => context.setWantClientAuth(true)
    }

    if (opts.excludeCiphers.nonEmpty) {
      context.setExcludeCipherSuites(opts.excludeCiphers: _*)
    }
    if (opts.excludeProtocols.nonEmpty) {
      context.setExcludeProtocols(opts.excludeProtocols: _*)
    }
    context
  }

  private def sslConnector(server: Server, opts: JettyOptions): ServerConnector = {
    val httpFactory = {
      val config = httpConfig(opts)
      config.setSecureScheme("https")
      config.setSecurePort(opts.sslPort)
      config.addCustomizer(new SecureRequestCustomizer)
      new HttpConnectionFactory(config)
    }
    val sslFactory = new SslConnectionFactory(sslContextFactory(opts), "http/1.1")
    val conn = serverConnector(server, Seq(sslFactory, httpFactory))
    conn.setPort(opts.sslPort)
    opts.host.foreach(conn.setHost)
    conn.setIdleTimeout(opts.maxIdleTime)
    conn
  }

  private def createServer(opts: JettyOptions): Server = {
    val server = new Server(createThreadPool(opts))
    if (opts.allowHttp) {
      server.addConnector(httpConnector(server, opts))
    }
    if (opts.allowSsl) {
      server.addConnector(sslConnector(server, opts))
    }
    server
  }

  /**
   * Create, configure and start a Jetty server instance and use it to run handler.
   */
  def run(handler: Handler, opts: JettyOptions): Server = {
    // wrap given handler in Jetty handler instance
    val ah = new AbstractHandler {
      override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
        val req: HttpRequest = Servlet.buildRequest(request)
        Servlet.updateServletResponse(response, handler(req))
        baseRequest.setHandled(true)
      }
    }

    val server = createServer(opts)
    server.setHandler(ah)
    opts.configFn(server)

    try {
      server.start()
      if (opts.join) {
        server.join()
      }
      server
    } catch {
      case e: Exception =>
        server.stop()
        throw e
    }
  }

}
