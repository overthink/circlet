package com.markfeeney.circlet

import java.io.{File, InputStream}

import scala.concurrent.Future
import scala.language.implicitConversions

sealed trait ResponseBody
object ResponseBody {
  final case class StringBody(value: String) extends ResponseBody
  final case class SeqBody[T](value: Seq[T]) extends ResponseBody
  final case class FileBody(value: File) extends ResponseBody
  final case class StreamBody(value: InputStream) extends ResponseBody
  final case class FutureBody(value: Future[ResponseBody]) extends ResponseBody

  // helper so you can set `body = "whatever"` in a response
  implicit def strToResponseBody(s: String): Option[ResponseBody] = {
    Option(s).map(StringBody)
  }

  implicit def futureToResponseBody(f: Future[ResponseBody]): Option[ResponseBody] = {
    Some(FutureBody(f))
  }
}
