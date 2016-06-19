package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.ResponseBody.StringBody
import com.markfeeney.circlet.TestUtils.{hwApp, request}
import com.markfeeney.circlet.{Handler, HttpMethod}
import com.markfeeney.circlet.Circlet.extractResponse
import org.scalatest.FunSuite

class HeadTest extends FunSuite {

  private val wrapped: Handler = Head.mw(hwApp)

  test("something responding to GET also responds to HEAD") {
    val getResp = extractResponse(hwApp(request(HttpMethod.Get, "/"))).get
    withClue("get expected response with GET") {
      assert(getResp.status == 200)
      assert(getResp.headers.get("X-Foo").contains(Vector("42")))
      assert(getResp.body.contains(StringBody("Hello world")))
    }

    val headResp = extractResponse(wrapped(request(HttpMethod.Head, "/"))).get
    withClue("response to HEAD is same as GET, sans body") {
      assert(headResp == getResp.copy(body = None))
    }
  }

}
