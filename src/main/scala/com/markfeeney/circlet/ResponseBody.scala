package com.markfeeney.circlet

import java.io.{InputStream, File}
import scala.language.implicitConversions

sealed trait ResponseBody
object ResponseBody {
  final case class StringBody(value: String) extends ResponseBody
  final case class SeqBody[T](value: Seq[T]) extends ResponseBody
  final case class FileBody(value: File) extends ResponseBody
  final case class StreamBody(value: InputStream) extends ResponseBody

  // helper so you can set `body = "whatever"` in a response
  implicit def strToResponseBody(s: String): Option[ResponseBody] = {
    Option(s).map(StringBody)
  }
}
