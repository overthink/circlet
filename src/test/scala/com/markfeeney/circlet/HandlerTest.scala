package com.markfeeney.circlet

import com.markfeeney.circlet.middleware.Head
import org.scalatest.FunSuite

class HandlerTest extends FunSuite {
  test("simple") {
    val app: Handler = request => {
      Response(status = 200, body = "simple handler response")
    }

    val app0 = Head.apply(app)

    val req = Request(
      uri = "/test",
      serverPort = 80,
      serverName = "foobar.com",
      remoteAddr = "localhost",
      requestMethod = HttpMethod.Head
    )

    val resp = app0(req)
    assert(resp.status == 200)
    assert(resp.body.isEmpty)

  }

  test("fancy") {
    val app: CpsHandler = (req, cont) => {
      println(s"got request $req")

      println("acquire expensive resource")
      val resp = Response(status = 200, body = "This is a body!")
      val ret = cont(resp)
      println("release expensive resource")
      ret
    }

    val app0: CpsHandler = Head.cps(app)

    val req = Request(
      uri = "/test",
      serverPort = 80,
      serverName = "foobar.com",
      remoteAddr = "localhost",
      requestMethod = HttpMethod.Head
    )

    app0(req, resp => {
      assert(resp.status == 200)
      assert(resp.body.isEmpty)
      Done
    })
  }
}
