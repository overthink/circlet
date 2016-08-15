package com.markfeeney.circlet

import java.security.KeyStore
import scala.language.implicitConversions
import com.markfeeney.circlet.JettyOptions.{ClientAuth, SslStoreConfig}
import org.eclipse.jetty.server.Server

/**
 * Options for configuring Jetty.
 *
 * @param join If true, calls join() on Server instance (blocking till server shuts down)
 * @param host The network interface connectors will bind to. If None or 0.0.0.0, binds to all interfaces.
 * @param httpPort A ServerConnector will be created listening on this port.
 * @param maxThreads Max threads in the threadpool used by connectors to run (eventually) the handler.
 * @param minThreads Minimum threads to keep alive in the Jetty threadpool.
 * @param daemonThreads If true, all threads in Jetty's threadpool will be daemon threads.
 * @param configFn A function to mess around with the Server instance before start is called.
 * @param maxIdleTime The max idle time for a connection (roughly Socket.setSoTimeout(int))
 * @param allowHttp If true an HTTP connector is setup listening on `port`
 * @param allowSsl If true an SSL connector is setup listing on `sslPort`
 * @param sslPort The port to listen for SSL connections if `enableSsl` is true.
 * @param sendDateHeader If true, include date in HTTP headers
 * @param outputBufferSize The size of the buffer into which the response is aggregated before
 *                         being sent to the client. Larger buffer is less likely to block
 *                         content producer, but could increase from client's perspective.
 * @param requestHeaderSize Max size of a request header. Larger sizes allow for more cookies or stuff encoded
 *                          in a URL.
 * @param responseHeaderSize Max size of a response header. Similar use cases as `requestHeaderSize`.
 * @param sendServerVersion If true, send the Server header in responses.
 * @param keyStore Specifies the keystore (private certs) to use for SSL connections
 * @param keyStorePassword Password for keystore
 * @param trustStore Specifies the keystore (public certs) to use for SSL connections
 * @param trustStorePassword Password for trustStore
 * @param clientAuth Policy for client SSL authentication (i.e. Need/Want).
 * @param excludeCiphers Cipher suites to exclude when using SSL
 * @param excludeProtocols Protocols to exclude when using SSL
 * @param webSockets Mapping of context path "e.g. /foo" to websocket instance.
 * @param maxWsIdleTime Maximum idle time for websocket connections.
 *
 */
case class JettyOptions(
  join: Boolean = true,
  host: Option[String] = None,
  httpPort: Int = 80,
  maxThreads: Int = 50,
  minThreads: Int = 8,
  daemonThreads: Boolean = false,
  configFn: Server => Unit = { _ => },
  maxIdleTime: Int = 200000,
  allowHttp: Boolean = true,
  allowSsl: Boolean = false,
  sslPort: Int = 443,
  sendDateHeader: Boolean = true,
  outputBufferSize: Int = 32768,
  requestHeaderSize: Int = 8192,
  responseHeaderSize: Int = 8192,
  sendServerVersion: Boolean = true,
  keyStore: Option[SslStoreConfig] = None,
  keyStorePassword: Option[String] = None,
  trustStore: Option[SslStoreConfig] = None,
  trustStorePassword: Option[String] = None,
  clientAuth: Option[ClientAuth] = None,
  excludeCiphers: Vector[String] = Vector.empty,
  excludeProtocols: Vector[String] = Vector.empty,
  webSockets: Map[String, JettyWebSocket] = Map.empty,
  maxWsIdleTime: Int = 600000
)

object JettyOptions {

  sealed trait SslStoreConfig

  object SslStoreConfig {
    final case class Path(path: String) extends SslStoreConfig
    final case class Instance(keystore: KeyStore) extends SslStoreConfig
    implicit def stringToPath(path: String): Option[SslStoreConfig] = {
      Some(Path(path))
    }
  }

  sealed trait ClientAuth
  object ClientAuth {
    case object Need extends ClientAuth
    case object Want extends ClientAuth
  }
}
