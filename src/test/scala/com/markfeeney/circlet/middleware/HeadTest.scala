package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.ResponseBody.StringBody
import com.markfeeney.circlet.ResponseHeaderValue.Single
import com.markfeeney.circlet.TestUtils.request
import com.markfeeney.circlet.{Handler, HttpMethod, Response}
import org.scalatest.FunSuite

class HeadTest extends FunSuite {

  private val testHandler: Handler = req => {
    req.requestMethod match {
      case HttpMethod.Get =>
        // Add a header so we can be sure this code executed even if body is remmoved
        Response(body = "Hello world", headers = Map("X-Foo" -> "42"))
      case _ =>
        Response(status = 404, body = "not found")
    }
  }

  private val wrapped: Handler = Head.apply(testHandler)

  test("something responding to GET also responds to HEAD") {
    val getResp = testHandler(request(HttpMethod.Get, "/"))
    withClue("get expected response with GET") {
      assert(getResp.status == 200)
      assert(getResp.headers.get("X-Foo").contains(Single("42")))
      assert(getResp.body.contains(StringBody("Hello world")))
    }

    val headResp = wrapped(request(HttpMethod.Head, "/"))
    withClue("response to HEAD is same as GET, sans body") {
      assert(headResp == getResp.copy(body = None))
    }

  }

}
