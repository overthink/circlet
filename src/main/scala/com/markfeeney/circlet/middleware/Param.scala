package com.markfeeney.circlet.middleware

import java.io.File
import scala.language.implicitConversions

/**
 * Represents a parameter parsed from a Request.  e.g. something from the query
 * string, form body, or multipart form body.
 */
sealed trait Param

final case class StrParam(value: Vector[String]) extends Param

/**
 * A parameter resulting from a multipart file upload.
 *
 * @param filename The name of the uploaded file
 * @param contentType Content type of uploaded file
 * @param tempFile The uploaded file on disk (temporary file)
 * @param size Size of uploaded file
 */
final case class FileParam(
  filename: String,
  contentType: String,
  tempFile: File,
  size: Long) extends Param

object Param {
  implicit def str2StrParam(s: String): StrParam = StrParam(Vector(s))
  implicit def vec2StrParam(xs: Vector[String]): StrParam = StrParam(xs)
}
