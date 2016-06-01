package com.markfeeney.circlet.middleware

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._
import java.nio.file.{Files, StandardCopyOption}
import com.markfeeney.circlet.CpsConverters._
import com.markfeeney.circlet.{Cleanly, CpsMiddleware, Middleware, Request}
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.fileupload.{FileItemIterator, FileItemStream, FileUpload, UploadContext}

/**
 * Provides middleware for parsing params from multipart/form-data request bodies.
 * This is normally used for accepting file uploads from web browsers.
 */
object MultipartParams {

  /**
   * Functionality for storing multipart file uploads.
   * (This is a trait largely for testability, but also a handy hook for storing uploads
   * in different ways.)
   */
  trait StorageEngine {
    /** Parse a FileItemStream into a named Param. */
    def parseFileItem(item: FileItemStream, encoding: Charset): Either[Exception, (String, Param)]

    /** Release any resources related to param. */
    def dispose(param: Param): Unit
  }

  object StorageEngine {

    /** Store file uploads in temp files. */
    object TempFile extends StorageEngine {

      /** Save the contents of `item` to a new temp file and return it. */
      private def saveFile(item: FileItemStream): Either[Exception, File] = {
        val temp = File.createTempFile("circlet-multipart-", null)
        val result =
          Cleanly(item.openStream())(_.close()) { stream =>
            Files.copy(stream, temp.toPath, StandardCopyOption.REPLACE_EXISTING)
            temp
          }
        // If the copy failed, clean up temp file -- there will be no other opportunity to do so
        if (result.isLeft) {
          temp.delete()
        }
        result
      }

      override def parseFileItem(item: FileItemStream, encoding: Charset): Either[Exception, (String, Param)] = {
        val name: String = item.getFieldName
        val parsed: Either[Exception, Param] =
          if (item.isFormField) {
            val str = Cleanly(item.openStream())(_.close()) { stream =>
              Streams.asString(stream, encoding.toString)
            }
            str.right.map(s => StrParam(Vector(s)))
          } else {
            saveFile(item).right.map { tempFile =>
              // for default content type: https://www.ietf.org/rfc/rfc2388.txt
              val ct = Option(item.getContentType).getOrElse("application/octet-stream")
              FileParam(item.getName, ct, tempFile, tempFile.length())
            }
          }
        parsed.right.map { value =>
          name -> value
        }
      }

      override def dispose(param: Param): Unit = {
        param match {
          case FileParam(_, _, tempFile, _) => tempFile.delete()
          case _ => // ignore
        }
      }

    }
  }

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

  private def parseMultipart(req: Request, encoding: Charset, storage: StorageEngine): Map[String, Param] = {
    if (isMultipartForm(req)) {
      val ctx = uploadContext(req, encoding)
      val iter: FileItemIterator = new FileUpload().getItemIterator(ctx)
      val parsed = fileItems(iter).map(storage.parseFileItem(_, encoding))

      // If any of the parsed params yielded an exception, then clean up any
      // allocated temp files from other params and rethrow exception
      parsed.find(_.isLeft).foreach { failed =>
        parsed.foreach {
          case Right((_, param)) => storage.dispose(param)
          case _ => // ignore
        }
        throw new RuntimeException("failed processing multipart param", failed.left.get)
      }

      parsed
        .filter(_.isRight) // defensive; any Left should throw above
        .map(_.right.get)
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

  private def addMultipart(req: Request, encoding: Option[Charset], storage: StorageEngine): Request = {
    val enc: Charset = encoding.orElse(req.characterEncoding).getOrElse(UTF_8)
    // Regular Params middleware may have parsed some params already; don't trample
    val params = Params.get(req).copy(multipartParams = parseMultipart(req, enc, storage))
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
   * @param storage Used to store file upload params. Default impl uses temp files on disk.
   */
  def wrapCps(
      encoding: Option[Charset] = None,
      storage: StorageEngine = StorageEngine.TempFile): CpsMiddleware = cpsHandler => (req, k) => {

    val req0 = addMultipart(req, encoding, storage)

    // Remember the params we just created so we can dispose of them when request done
    val params = Params.get(req0).multipartParams.values

    cpsHandler(req0, resp => {
      try {
        k(resp)
      } finally {
        params.foreach(storage.dispose)
      }
    })
  }

  /** Non-CPS helper. See `wrapCps()`. */
  def wrap(
      encoding: Option[Charset] = None,
      storage: StorageEngine = StorageEngine.TempFile): Middleware = {
    wrapCps(encoding, storage)
  }

}
