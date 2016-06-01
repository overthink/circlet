package com.markfeeney.circlet.middleware

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import com.markfeeney.circlet.middleware.MultipartParams.StorageEngine
import com.markfeeney.circlet.{Handler, HttpMethod, Response, TestUtils, Util}
import org.apache.commons.fileupload.FileItemStream
import org.scalatest.FunSuite

class MultipartParamsTest extends FunSuite {

  private val body = "--XXXX\r\n" +
    "Content-Disposition: form-data; name=\"upload\"; filename=\"test.txt\"\r\n" +
    "Content-Type: text/plain\r\n\r\n" +
    "foo\nbar!\r\n" +
    "--XXXX\r\n" +
    "Content-Disposition: form-data; name=\"upload2\"; filename=\"foo.whatev\"\r\n\r\n" +
    "ᚠᛇᚻ᛫ᛒᛦᚦ᛫\nᚠᚱᚩᚠᚢᚱ\r\n" +
    "--XXXX\r\n" +
    "Content-Disposition: form-data; name=\"baz\"\r\n\r\n" +
    "quux\r\n" +
    "--XXXX--"

  test("basic multipart") {
    val request = TestUtils.request(HttpMethod.Get, "/test")
      .copy(body = Some(Util.stringInputStream(body)))
      .setContentType("multipart/form-data; boundary=XXXX")
      .setContentLength(body.getBytes(StandardCharsets.UTF_8).length)

    val tempFiles: ListBuffer[File] = ListBuffer.empty

    val h: Handler = req => {
      val ps = Params.get(req)
      assert(ps.multipartParams.size == 3)
      assert(ps.multipartParams.size == ps.all.size)
      assert(ps.multipartParams.get("baz").contains(StrParam(Vector("quux"))))
      ps.multipartParams.get("upload") foreach {
        case FileParam(fileName, contentType, tempFile0, size) =>
          tempFiles.append(tempFile0)
          assert(fileName == "test.txt")
          assert(contentType == "text/plain")
          assert(tempFile0.exists())
          assert(Source.fromFile(tempFile0, "UTF-8").mkString == "foo\nbar!")
          assert(size == 8)
        case _ => fail("upload param should be FileParam")
      }
      ps.multipartParams.get("upload2") foreach {
        case FileParam(fileName, contentType, tempFile0, size) =>
          tempFiles.append(tempFile0)
          assert(fileName == "foo.whatev")
          assert(contentType == "application/octet-stream", "If not set, use default content-type for file upload")
          assert(tempFile0.exists())
          assert(Source.fromFile(tempFile0, "UTF-8").mkString == "ᚠᛇᚻ᛫ᛒᛦᚦ᛫\nᚠᚱᚩᚠᚢᚱ")
          assert(size == 43) // note: pasting these runes into vim yields 44 bytes without `:set binary` - http://stackoverflow.com/a/16114535/69689
        case _ => fail("upload2 param should be FileParam")
      }
      Response()
    }

    val app = MultipartParams.wrap()(h)
    app(request)

    withClue("after request complete") {
      assert(tempFiles.nonEmpty)
      assert(tempFiles.count(_.exists()) == 0, "temp file was removed after request completed")
    }

  }

  test("multiple file uploads with failure") {
    val request = TestUtils.request(HttpMethod.Get, "/test")
      .copy(body = Some(Util.stringInputStream(body)))
      .setContentType("multipart/form-data; boundary=XXXX")
      .setContentLength(body.getBytes(StandardCharsets.UTF_8).length)

    val h: Handler = req => Response()

    var disposeCount = 0
    val disposedParams: ListBuffer[Param] = ListBuffer.empty
    var parseFileCount = 0

    val failingStorage = new StorageEngine {
      override def dispose(param: Param): Unit = {
        disposeCount += 1
        disposedParams.append(param)
        StorageEngine.TempFile.dispose(param)
      }
      override def parseFileItem(item: FileItemStream, encoding: Charset): Either[Exception, (String, Param)] = {
        parseFileCount += 1
        if (parseFileCount == 1) {
          StorageEngine.TempFile.parseFileItem(item, encoding)
        } else {
          Left(new RuntimeException("forced storage engine failure"))
        }
      }
    }

    val app = MultipartParams.wrap(storage = failingStorage)(h)
    try {
      app(request)
      fail("should have thrown")
    } catch {
      case e: RuntimeException => // expected
    } finally {
      assert(parseFileCount == 3, "parseFileItem should have been called on all multipart params")
      assert(disposeCount == 1, "dispose should have been called for the one FileParam that didn't fail")
      assert(disposedParams.head.asInstanceOf[FileParam].filename == "test.txt")
    }
  }


}
