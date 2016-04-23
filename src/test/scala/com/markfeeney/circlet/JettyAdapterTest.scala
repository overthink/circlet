package com.markfeeney.circlet

import java.net.ServerSocket
import com.markfeeney.circlet.ResponseBody.StringBody
import com.mashape.unirest.http.Unirest
import org.eclipse.jetty.server.Server
import org.scalatest.FunSuite

class JettyAdapterTest extends FunSuite {

  private def findFreePort: Int = {
    Cleanly(new ServerSocket(0))(_.close)(_.getLocalPort).right.get
  }

  private case class TestServer(server: Server, port: Int)

  private def testServer(h: Handler)(f: TestServer => Unit): Unit = {

    val opts = JettyOptions(
      join = false,
      port = findFreePort
    )

    val result = Cleanly(JettyAdapter.run(h, opts))(_.stop()) { server =>
      f(TestServer(server, opts.port))
    }

    result match {
      case Left(e) => throw e
      case Right(_) =>
    }
  }

  private def helloWorld: Handler = { req =>
    Some(HttpResponse(status = 200, body = Some(StringBody("Hello world"))))
  }

  test("Jetty server start/running/stop works") {
    testServer(helloWorld) { case TestServer(server, _) =>
      assert(server.isRunning)
    }
  }

  test("Can create and stop a Jetty server") {
    testServer(helloWorld) { case TestServer(server, port) =>
      assert(server.isRunning)
      val resp = Unirest.get("http://localhost:" + port).asString()
      assert(resp.getBody == "Hello world")
    }
  }

}
