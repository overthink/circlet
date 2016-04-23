package com.markfeeney.circlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.eclipse.jetty.server._
import org.eclipse.jetty.server.handler.AbstractHandler
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
    println("setting port on connector: " + opts.port)
    connector.setPort(opts.port)
    opts.host.foreach(connector.setHost)
    connector.setIdleTimeout(opts.maxIdleTimeout)
    connector
  }

  private def createServer(opts: JettyOptions): Server = {
    val server = new Server(createThreadPool(opts))
    if (opts.enableHttp) {
      server.addConnector(httpConnector(server, opts))
    }
    if (opts.enableSsl) {
      ???
    }
    server
  }

  /**
   * Create, configure and start a Jetty server instance and use it to run handler.
   */
  def run(handler: Handler, opts: JettyOptions): Server = {
    // wrap given handler in Jetty handler instance
    println("running jetty with opts: " + opts)
    val ah = new AbstractHandler {
      override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
        val req: HttpRequest = Servlet.buildRequest(request)
        handler(req).foreach { resp =>
          Servlet.updateServletResponse(response, resp)
          baseRequest.setHandled(true)
        }
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
