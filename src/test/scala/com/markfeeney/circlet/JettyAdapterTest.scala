package com.markfeeney.circlet

import java.net.ServerSocket
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import scala.collection.JavaConverters._
import com.mashape.unirest.http
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, TrustSelfSignedStrategy}
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import org.eclipse.jetty.server.{Request=>JettyRequest, _}
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.scalatest.FunSuite

class JettyAdapterTest extends FunSuite {

  // disable logging from Jetty
  org.eclipse.jetty.util.log.Log.setLog(new NoJettyLogging)

  def makeUnirestAllowSelfSignedCerts(): Unit = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    val sslsf = new SSLConnectionSocketFactory(builder.build)
    val httpclient = HttpClients.custom.setSSLSocketFactory(sslsf).build()
    Unirest.setHttpClient(httpclient)
  }
  makeUnirestAllowSelfSignedCerts()

  private def findFreePort: Int = {
    Cleanly(new ServerSocket(0))(_.close)(_.getLocalPort).right.get
  }

  private case class TestServer(server: Server, opts: JettyOptions)

  private def testServer(h: Handler, opts: JettyOptions)(f: TestServer => Unit): Unit = {
    val opts0 = opts.copy(
      join = false,
      httpPort = findFreePort,
      sslPort = if (opts.allowSsl) findFreePort else opts.sslPort // if testing ssl, generate random ssl port
    )
    val result = Cleanly(JettyAdapter.run(h, opts0))(_.stop()) { server =>
      f(TestServer(server, opts0))
    }
    result.left.foreach(e => throw e)
  }

  private def testServer(opts: JettyOptions)(f: TestServer => Unit): Unit = {
    testServer(helloWorld, opts)(f)
  }

  private def helloWorld: Handler = { _ =>
    Response(body = "Hello world")
  }

  sealed trait Scheme
  case object Http extends Scheme
  case object Https extends Scheme

  private def get(scheme: Scheme, port: Int): http.HttpResponse[String] = {
    val scheme0 = scheme match {
      case Http => "http"
      case Https => "https"
    }
    Unirest.get(f"$scheme0%s://localhost:$port%d").asString
  }

  private def assertHelloWorld(scheme: Scheme, port: Int): Unit =  {
    assert(get(scheme, port).getBody == "Hello world")
  }

  test("http smoke test") {
    testServer(JettyOptions()) { case TestServer(server, opts) =>
      assertHelloWorld(Http, opts.httpPort)
      withClue("ssl disabled by default") {
        val e = intercept[UnirestException](get(Https, opts.sslPort))
        assert(e.getMessage.contains("Connection refused"))
      }
    }
  }

  private def enableSsl(opts: JettyOptions): JettyOptions = {
    opts.copy(
      allowSsl = true,
      keyStore = "src/test/resources/test-keystore",
      keyStorePassword = Some("password")
    )
  }

  test("ssl (only) smoke test") {
    testServer(enableSsl(JettyOptions(allowHttp = false))) { case TestServer(server, opts) =>
      assertHelloWorld(Https, opts.sslPort)
      withClue("plaintext connections to ssl port don't work") {
        val e = intercept[UnirestException](get(Http, opts.sslPort))
        assert(e.getMessage.contains("failed to respond"))
      }
      withClue("plain HTTP (on correct port) disabled") {
        val e = intercept[UnirestException](get(Http, opts.httpPort))
        assert(e.getMessage.contains("Connection refused"))
      }
    }
  }

  test("http and ssl both enabled") {
    testServer(enableSsl(JettyOptions())) { case TestServer(server, opts) =>
      assertHelloWorld(Http, opts.httpPort)
      assertHelloWorld(Https, opts.sslPort)
    }
  }

  private def factories(conn: Connector): Vector[ConnectionFactory] = {
    conn.getConnectionFactories.asScala.toVector
  }

  test("server connectors are setup correctly") {
    testServer(JettyOptions(allowHttp = false)) { case TestServer(server, _) =>
      // bizarre case, theoretically connectors could be added by configFn
      assert(0 == server.getConnectors.length)
    }
    testServer(JettyOptions(allowHttp = true)) { case TestServer(server, _) =>
      assert(1 == server.getConnectors.length)
      assert(factories(server.getConnectors.head).head.isInstanceOf[HttpConnectionFactory])
    }
    testServer(JettyOptions(allowHttp = true, allowSsl = true)) { case TestServer(server, _) =>
      val cons: Array[Connector] = server.getConnectors
      val httpConn = cons(0)
      val sslConn = cons(1)
      assert(2 == cons.length, "one connector for http, one for https")
      val httpFactories = factories(httpConn)
      assert(1 == httpFactories.length)
      assert(httpFactories.head.isInstanceOf[HttpConnectionFactory])
      val sslFactories = factories(sslConn)
      assert(2 == sslFactories.length)
      assert(sslFactories(0).isInstanceOf[SslConnectionFactory])
      assert(sslFactories(1).isInstanceOf[HttpConnectionFactory])
    }
  }

  test("thread pool non-daemon by default") {
    testServer(JettyOptions()) { case TestServer(server, _) =>
      assert(server.getThreadPool.isInstanceOf[QueuedThreadPool])
      val p = server.getThreadPool.asInstanceOf[QueuedThreadPool]
      assert(!p.isDaemon)
    }
  }

  // create server with opts, then inspect server to see if settings are actually applied
  private def assertThreadPool(opts: JettyOptions): Unit = {
    testServer(opts) { case TestServer(server, _) =>
      assert(server.getThreadPool.isInstanceOf[QueuedThreadPool])
      val p = server.getThreadPool.asInstanceOf[QueuedThreadPool]
      assert(p.getMaxThreads == opts.maxThreads)
      assert(p.getMinThreads == opts.minThreads)
      assert(p.isDaemon == opts.daemonThreads)
    }
  }

  test("thread pool settings can be changed") {
    assertThreadPool(JettyOptions()) // defaults
    assertThreadPool(JettyOptions(maxThreads = 99, minThreads = 1, daemonThreads = true))
  }

  test("configFn can override other settings") {
    val noopHandler= new AbstractHandler {
      override def handle(target: String, baseRequest: JettyRequest, request: HttpServletRequest, response: HttpServletResponse): Unit = {}
    }
    val f: Server => Unit = { s =>
      s.getThreadPool.asInstanceOf[QueuedThreadPool].setDaemon(false)
      s.setHandler(noopHandler)
    }
    val opts = JettyOptions(daemonThreads = true, configFn = f)
    testServer(helloWorld, opts) { case TestServer(server, _) =>
      assert(!server.getThreadPool.asInstanceOf[QueuedThreadPool].isDaemon)
      assert(server.getHandler eq noopHandler, "Handler is not the default helloWorld handler")
    }
  }

  // ensure config object matches JettyOptions
  private def assertHttpConfig(opts: JettyOptions, config: HttpConfiguration): Unit = {
    assert(config.getSendDateHeader == opts.sendDateHeader)
    assert(config.getOutputBufferSize == opts.outputBufferSize)
    assert(config.getRequestHeaderSize == opts.requestHeaderSize)
    assert(config.getResponseHeaderSize == opts.responseHeaderSize)
    assert(config.getSendServerVersion == opts.sendServerVersion)
  }

  test("can change http config") {
    val opts = JettyOptions(
      allowHttp = true,
      allowSsl = true,
      sendDateHeader = false,
      outputBufferSize = 9000,
      requestHeaderSize = 10000,
      responseHeaderSize = 11000,
      sendServerVersion = false
    )
    testServer(opts) { case TestServer(server, _) =>
      val cons: Array[Connector] = server.getConnectors
      val httpConn = cons(0)
      val sslConn = cons(1)
      assert(2 == cons.length, "one connector for http, one for https")
      val httpFactories = factories(httpConn)
      assert(1 == httpFactories.length)
      assert(httpFactories.head.isInstanceOf[HttpConnectionFactory])
      assertHttpConfig(opts, httpFactories.head.asInstanceOf[HttpConnectionFactory].getHttpConfiguration)
      val sslFactories = factories(sslConn)
      assert(2 == sslFactories.length)
      assert(sslFactories(0).isInstanceOf[SslConnectionFactory])
      assert(sslFactories(1).isInstanceOf[HttpConnectionFactory])
      assertHttpConfig(opts, sslFactories(1).asInstanceOf[HttpConnectionFactory].getHttpConfiguration)
    }
  }
}
