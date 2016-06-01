package com.markfeeney.circlet.middleware

import java.io.File
import java.nio.charset.StandardCharsets
import scala.io.Source
import com.markfeeney.circlet.{Handler, HttpMethod, Response, TestUtils, Util}
import org.scalatest.FunSuite

class MultipartParamsTest extends FunSuite {

  test("basic multipart") {

    val body = "--XXXX\r\n" +
      "Content-Disposition: form-data; name=\"upload\"; filename=\"test.txt\"\r\n" +
      "Content-Type: text/plain\r\n\r\n" +
      "foo\nbar!\r\n" +
      "--XXXX\r\n" +
      "Content-Disposition: form-data; name=\"baz\"\r\n\r\n" +
      "quux\r\n" +
      "--XXXX--"

    val request = TestUtils.request(HttpMethod.Get, "/test")
      .copy(body = Some(Util.stringInputStream(body)))
      .setContentType("multipart/form-data; boundary=XXXX")
      .setContentLength(body.getBytes(StandardCharsets.UTF_8).length)

    var tempFile: File = null

    val h: Handler = req => {
      val ps = Params.get(req)
      assert(ps.multipartParams.size == 2)
      assert(ps.multipartParams.size == ps.all.size)
      assert(ps.multipartParams.get("baz").contains(StrParam(Vector("quux"))))
      ps.multipartParams.get("upload") foreach {
        case FileParam(fileName, contentType, tempFile0, size) =>
          tempFile = tempFile0
          assert(fileName == "test.txt")
          assert(contentType == "text/plain")
          assert(tempFile0.exists())
          assert(Source.fromFile(tempFile, "UTF-8").mkString == "foo\nbar!")
          assert(size == 8)
        case _ => fail("upload param should be FileParam")
      }
      Response()
    }

    val app = MultipartParams.wrap()(h)
    app(request)

    withClue("after request complete") {
      assert(tempFile != null)
      assert(!tempFile.exists(), "temp file was removed after request completed")
    }

  }

}
