package com.markfeeney.circlet

import java.net.ServerSocket
import com.mashape.unirest.http
import com.mashape.unirest.http.Unirest
import org.eclipse.jetty.server.Server
import org.scalatest.FunSuite

class JettyAdapterTest extends FunSuite {

  private def findFreePort: Int = {
    Cleanly(new ServerSocket(0))(_.close)(_.getLocalPort).right.get
  }

  private case class TestServer(server: Server, port: Int)

  private def testServer(h: Handler, opts: JettyOptions)(f: TestServer => Unit): Unit = {
    val opts0 = opts.copy(
      join = false,
      port = findFreePort
    )
    val result = Cleanly(JettyAdapter.run(h, opts0))(_.stop()) { server =>
      f(TestServer(server, opts0.port))
    }
    result.left.foreach(e => throw e)
  }

  private def testServer(h: Handler)(f: TestServer => Unit): Unit = {
    testServer(h, JettyOptions())(f)
  }

//  private def testServer0(f: TestServer => Unit): Unit = {
//    testServer(helloWorld)(f)
//  }

  private def helloWorld: Handler = { _ =>
    HttpResponse(body = "Hello world")
  }

  private def get(port: Int): http.HttpResponse[String] = Unirest.get("http://localhost:" + port).asString

  test("Jetty server start/running/stop works") {
    testServer(helloWorld) { case TestServer(server, _) =>
      assert(server.isRunning)
    }
  }

  test("default config has a single working connector") {
    testServer(helloWorld) { case TestServer(server, port) =>
      assert(get(port).getBody == "Hello world")
    }
  }

}
