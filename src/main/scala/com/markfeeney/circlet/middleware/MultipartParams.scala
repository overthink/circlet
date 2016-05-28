package com.markfeeney.circlet.middleware

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._
import java.nio.file.{StandardCopyOption, CopyOption, Files}
import com.markfeeney.circlet.{Middleware, Cleanly, CpsMiddleware, Request}
import com.markfeeney.circlet.CpsConverters._
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.fileupload.{FileItemIterator, FileItemStream, FileUpload, UploadContext}

object MultipartParams {

  private def isMultipartForm(req: Request): Boolean = {
    req.contentType.exists(_.startsWith("multipart/form-data"))
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
    val temp = File.createTempFile("circlet-multipart-", null)
    val stream = item.openStream()
    try {
      Files.copy(stream, temp.toPath) // , StandardCopyOption.REPLACE_EXISTING)
      temp
    } catch {
      case e: Exception =>
        // TODO: this is not quite enough... consider case where some file uploads
        // parse but others throw -- the parsed ones will be stranded on disk since
        // the higher-level cleanup only works if addMultipart() returns successfully
        temp.delete()
        throw e
    }
    finally {
      stream.close()
    }
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
        // better to use Either or Option? Not convinced
        str.right.get
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

  /**
   * Add parsed params from multipart form posts to the request. Use `Params.get(req)` to
   * get access to them. Returns new Request.  File uploads are stored as temp files
   * (see `FileParam`) for the duration of the request, then deleted.
   *
   * @param encoding The encoding used for multipart parsting. If not specified,
   *                 uses the request character encoding, or UTF-8 if no request
   *                 encoding can be found.
   */
  def wrapCps(encoding: Option[Charset] = None): CpsMiddleware = cpsHandler => (req, k) => {
    val req0 = addMultipart(req, encoding)

    // Remember any temp files we created so we can clean them after other handlers are done
    val tempFiles = Params.get(req0)
      .multipartParams
      .values
      .collect { case FileParam(_, _, tempFile, _) => tempFile }
      .toVector

    cpsHandler(req0, resp => {
      try {
        k(resp)
      } finally {
        tempFiles.foreach(_.delete())
      }
    })
  }

  def wrap(encoding: Option[Charset] = None): Middleware = wrapCps(encoding)

}
