package com.markfeeney.circlet

import com.markfeeney.circlet.middleware.Head
import org.scalatest.FunSuite

class HandlerTest extends FunSuite {
  test("foo") {
    assert(true)
  }

  test("fancy") {
    val app: CpsHandler = { case (req, cont) =>
      println(s"got request $req")

      println("acquire expensive resource")
      val resp = HttpResponse(status = 200, body = "This is a body!")
      val ret = cont(resp)
      println("release expensive resource")
      ret
    }

    val app0: CpsHandler = Head.apply(app)

    val req = HttpRequest(
      uri = "/test",
      serverPort = 80,
      serverName = "foobar.com",
      remoteAddr = "localhost",
      requestMethod = HttpMethod.Head
    )

    app0(req, { resp =>
      println(s"Got resp $resp")
      Done
    })
  }
}
