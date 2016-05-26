package com.markfeeney.circlet.middleware

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._
import java.io.File
import java.nio.file.Files
import com.markfeeney.circlet.{Response, Done, CpsMiddleware, Cleanly, Middleware, Request}
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.fileupload.{FileItemIterator, FileItemStream, FileUpload, UploadContext}

object MultipartParams {

  private def isMultipartForm(req: Request): Boolean = {
    req.contentType.contains("multipart/form-data")
  }

  private def uploadContext(req: Request, encoding: Charset): UploadContext = {
    new UploadContext {
      override def contentLength(): Long = req.contentLength.getOrElse(-1)
      override def getCharacterEncoding: String = encoding.toString
      override def getContentLength: Int = req.contentLength.map(_.toInt).getOrElse(-1)
      override def getContentType: String = req.contentType.orNull
      override def getInputStream: InputStream = req.body.get
    }
  }

  private def fileItems(it: FileItemIterator): Stream[FileItemStream] = {
    if (it.hasNext) {
      Stream.cons(it.next(), fileItems(it))
    } else {
      Stream.empty
    }
  }

  /** Save the contents of `item` to a new temp file and return the temp file. */
  private def saveFile(item: FileItemStream): File = {
    val result: Either[Exception, File] =
      Cleanly(item.openStream())(_.close()) { stream =>
        val temp = File.createTempFile("circlet-multipart-", null)
        Files.copy(stream, temp.toPath)
        temp
      }
    result.right.get // TODO: fn should return Option[File]
  }

  /**
   * Parse `item` into a key value pair.
   */
  private def parseFileItem(item: FileItemStream, encoding: Charset): (String, Param) = {
    val name = item.getFieldName
    val value: Param =
      if (item.isFormField) {
        val str = Cleanly(item.openStream())(_.close()) { stream =>
          Streams.asString(stream, encoding.toString)
        }
        str.right.get  // TODO: fn should return Option[(String, Param)]
      } else {
        val tempFile = saveFile(item)
        FileParam(item.getName, item.getContentType, tempFile, tempFile.length())
      }
    name -> value
  }

  private def parseMultipart(req: Request, encoding: Charset): Map[String, Param] = {
    if (isMultipartForm(req)) {
      val ctx = uploadContext(req, encoding)
      val iter: FileItemIterator = new FileUpload().getItemIterator(ctx)
      fileItems(iter)
        .map(item => parseFileItem(item, encoding))
        .foldLeft(Map[String, Param]()) { case (acc, (k, v)) =>
          (acc.get(k), v) match {
            case (Some(StrParam(xs)), StrParam(ys)) =>
              acc.updated(k, StrParam(xs ++ ys))
            case (_, other) =>
              acc.updated(k, other)
          }
        }
    } else {
      Map.empty
    }
  }

  private def addMultipart(req: Request, encoding: Option[Charset]): Request = {
    val enc: Charset = encoding.orElse(req.characterEncoding).getOrElse(UTF_8)
    // Regular Params middleware may have parsed some params already; don't trample
    val params = Params.get(req).copy(multipartParams = parseMultipart(req, enc))
    Params.set(req, params)
  }

  def wrapCps: CpsMiddleware = handler => (req, k) => {
    ???
  }

  /**
   * Add parsed params from multipart form posts to the request. Use `Params.get(req)` to
   * get access to them. Returns new Request.
   *
   * @param encoding The encoding used for multipart parsting. If not specified,
   *                 uses the request character encoding, or UTF-8 if no request
   *                 encoding can be found.
   */
  def wrap(encoding: Option[Charset] = None): Middleware = handler => req => {
    handler(addMultipart(req, encoding))
  }

}
