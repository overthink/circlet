package com.markfeeney.circlet

import java.net.ServerSocket
import com.markfeeney.circlet.ResponseBody.StringBody
import com.mashape.unirest.http.Unirest
import org.eclipse.jetty.server.Server
import org.scalatest.FunSuite

class JettyAdapterTest extends FunSuite {

  private def findFreePort: Int = {
    // not foolproof, but not bad
    val ss = new ServerSocket(0)
    ss.getLocalPort
  }

  private case class TestServer(server: Server, port: Int)

  private def testServer(h: Handler)(f: TestServer => Unit): Unit = {

    val opts = JettyOptions(join = false, port = findFreePort)
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

  ignore("Can create and stop a Jetty server") {
    testServer(helloWorld) { case TestServer(server, port) =>
      assert(server.isRunning)
      Unirest.setTimeouts(1000, 1000) // TODO temp hack
      val resp = Unirest.get("http://localhost:" + port).asString()
      // TODO: getting empty server body
      assert(resp.getBody == "Hello world")
    }
  }

}
