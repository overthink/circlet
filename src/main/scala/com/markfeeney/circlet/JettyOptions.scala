package com.markfeeney.circlet

import org.eclipse.jetty.server.Server

/**
 * Options for configuring Jetty.
 *
 * @param join If true, calls join() on Server instance (blocking till server shuts down)
 * @param host The network interface connectors will bind to. If None or 0.0.0.0, binds to all interfaces.
 * @param port A ServerConnector will be created listening on this port.
 * @param maxThreads Max threads in the threadpool used by connectors to run (eventually) the handler.
 * @param minThreads Minimum threads to keep alive in the Jetty threadpool.
 * @param daemonThreads If true, all threads in Jetty's threadpool will be daemon threads.
 * @param configFn A function to mess around with the Server instance before start is called.
 * @param maxIdleTimeout The max idle time for a connection (roughly Socket.setSoTimeout(int))
 * @param enableHttp If true an HTTP connector is setup listening on `port`
 * @param enableSsl If true an SSL connector is setup listing on `sslPort`
 * @param sslPort The port to listen for SSL connections if `enableSsl` is true.
 * @param sendDateHeader If true, include date in HTTP headers
 * @param outputBufferSize The size of the buffer into which the response is aggregated before
 *                         being sent to the client. Larger buffer is less likely to block
 *                         content producer, but could increase from client's perspective.
 * @param requestHeaderSize Max size of a request header. Larger sizes allow for more cookies or stuff encoded
 *                          in a URL.
 * @param responseHeaderSize Max size of a response header. Similar use cases as `requestHeaderSize`.
 * @param sendServerVersion If true, send the Server header in responses.
 */
case class JettyOptions(
  join: Boolean = true,
  host: Option[String] = None,
  port: Int = 80,
  maxThreads: Int = 50,
  minThreads: Int = 8,
  daemonThreads: Boolean = false,
  configFn: Server => Unit = { _ => },
  maxIdleTimeout: Int = 200000,
  enableHttp: Boolean = true,
  enableSsl: Boolean = false,
  sslPort: Int = 443,
  sendDateHeader: Boolean = true,
  outputBufferSize: Int = 32768,
  requestHeaderSize: Int = 8192,
  responseHeaderSize: Int = 8192,
  sendServerVersion: Boolean = true
)
