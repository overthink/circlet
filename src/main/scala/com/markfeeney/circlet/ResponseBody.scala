package com.markfeeney.circlet

import java.io.{InputStream, File}
import scala.language.implicitConversions

sealed trait ResponseBody
object ResponseBody {
  case class StringBody(value: String) extends ResponseBody
  case class SeqBody(value: Seq[AnyRef]) extends ResponseBody
  case class FileBody(value: File) extends ResponseBody
  case class StreamBody(value: InputStream) extends ResponseBody

  // helper so you can set `body = "whatever"` in a response
  implicit def strToResponseBody(s: String): Option[ResponseBody] = {
    Option(s).map(StringBody)
  }
}
