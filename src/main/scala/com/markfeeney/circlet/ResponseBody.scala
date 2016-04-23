package com.markfeeney.circlet

import java.io.{InputStream, File}

sealed trait ResponseBody
object ResponseBody {
  case class StringBody(value: String) extends ResponseBody
  case class SeqBody(value: Seq[AnyRef]) extends ResponseBody
  case class FileBody(value: File) extends ResponseBody
  case class StreamBody(value: InputStream) extends ResponseBody
}
