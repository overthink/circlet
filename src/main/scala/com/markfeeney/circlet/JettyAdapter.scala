package com.markfeeney.circlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ThreadPool}



/**
 * Functionality for running handlers on Jetty.
 */
object JettyAdapter {

  private def createThreadPool(options: JettyOptions): ThreadPool = {
    new QueuedThreadPool(options.maxThreads)
  }

  private def createServer(options: JettyOptions): Server = {
    val server = new Server(createThreadPool(options))
    // TODO: more config
    server
  }

  /**
   * Create, configure and start a Jetty server instance and use it to run handler.
   */
  def run(handler: Handler, options: JettyOptions): Server = {
    // wrap given handler in Jetty handler instance
    val ah = new AbstractHandler {
      override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
        val req: HttpRequest = Servlet.buildRequest(request)
        handler(req).foreach { resp =>
          Servlet.updateServletResponse(response, resp)
          baseRequest.setHandled(true)
        }
      }
    }

    val server = createServer(options)
    server.setHandler(ah)
    try {
      server.start()
      if (options.join) {
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
