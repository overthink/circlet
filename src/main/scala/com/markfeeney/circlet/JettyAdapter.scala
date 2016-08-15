package com.markfeeney.circlet

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.markfeeney.circlet.JettyOptions.ClientAuth.{Need, Want}
import com.markfeeney.circlet.JettyOptions.SslStoreConfig.{Instance, Path}
import org.eclipse.jetty.server.handler.{AbstractHandler, ContextHandler, HandlerList}
import org.eclipse.jetty.server.{Request => JettyRequest, _}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ThreadPool}
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.{ServletUpgradeRequest, ServletUpgradeResponse, WebSocketCreator, WebSocketServletFactory}

/**
 * Functionality for running handlers on Jetty.
 * Largely a port of https://github.com/ring-clojure/ring/blob/4a3584570ad9e7b17f6b1c8a2a17934c1682f77d/ring-jetty-adapter/src/ring/adapter/jetty.clj
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

  private def wsAdapter(ws: JettyWebSocket) = new WebSocketAdapter {
    override def onWebSocketConnect(s: Session): Unit = {
      super.onWebSocketConnect(s)
      ws.onConnect(s)
    }

    override def onWebSocketError(cause: Throwable): Unit = {
      ws.onError(this.getSession, cause)
    }

    override def onWebSocketText(message: String): Unit = {
      ws.onText(this.getSession, message)
    }

    override def onWebSocketBinary(payload: Array[Byte], offset: Int, len: Int): Unit = {
      ws.onBytes(this.getSession, payload, offset, len)
    }

    override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
      try {
        ws.onClose(this.getSession, statusCode, reason)
      } finally {
        super.onWebSocketClose(statusCode, reason)
      }
    }
  }

  private def wsCreator(ws: JettyWebSocket): WebSocketCreator = {
    new WebSocketCreator {
      override def createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): AnyRef = {
        wsAdapter(ws)
      }
    }
  }

  private def wsHandler(ws: JettyWebSocket, maxWsIdleTime: Int): AbstractHandler = {
    new WebSocketHandler() {
      override def configure(factory: WebSocketServletFactory): Unit = {
        factory.getPolicy.setIdleTimeout(maxWsIdleTime)
        factory.setCreator(wsCreator(ws))
      }
    }
  }

  private def wsHandlers(opts: JettyOptions): Seq[ContextHandler] = {
    opts.webSockets.map { case (path, ws) =>
      val ctx = new ContextHandler
      ctx.setContextPath(path)
      ctx.setHandler(wsHandler(ws, opts.maxWsIdleTime))
      ctx
    }.toSeq
  }

  /**
   * Create, configure and start a Jetty server instance and use it to run handler.
   */
  def run(handler: Handler, opts: JettyOptions = JettyOptions()): Server = {

    // The main app handler gets wrapped in a single Jetty handler instance...
    val ah = new AbstractHandler {
      override def handle(
          target: String,
          baseRequest: JettyRequest,
          request: HttpServletRequest,
          response: HttpServletResponse): Unit = {
        val req: Request = Servlet.buildRequest(request)
        handler(req) { optResp =>
          val resp = optResp.getOrElse {
            // TBD if this is a good way to handle this case
            Response(body = "No response generated", status = 500)
          }
          Servlet.updateServletResponse(response, resp)
          Sent
        }
        baseRequest.setHandled(true)
      }
    }

    // ... then each websocket also gets its own handler (kind of a bolted-on
    // approach, but it works for now). We build a big list of all handlers
    // and register them with the server below.  Approach borrowed from
    // https://github.com/sunng87/ring-jetty9-adapter
    val allHandlers = (wsHandlers(opts) :+ ah)
      .foldLeft(new HandlerList) { (acc, h) =>
        acc.addHandler(h)
        acc
      }

    val server = createServer(opts)
    server.setHandler(allHandlers)
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
