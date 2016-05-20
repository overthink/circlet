package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.ResponseBody.StringBody
import com.markfeeney.circlet.TestUtils.{hwApp, request}
import com.markfeeney.circlet.{Handler, HttpMethod}
import org.scalatest.FunSuite

class HeadTest extends FunSuite {

  private val wrapped: Handler = Head.wrap(hwApp)

  test("something responding to GET also responds to HEAD") {
    val getResp = hwApp(request(HttpMethod.Get, "/"))
    withClue("get expected response with GET") {
      assert(getResp.status == 200)
      assert(getResp.headers.get("X-Foo").contains(Vector("42")))
      assert(getResp.body.contains(StringBody("Hello world")))
    }

    val headResp = wrapped(request(HttpMethod.Head, "/"))
    withClue("response to HEAD is same as GET, sans body") {
      assert(headResp == getResp.copy(body = None))
    }
  }

}
