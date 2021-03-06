package com.markfeeney.circlet

import java.io.{ByteArrayInputStream, InputStream}
import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Try

/**
 * Grab-bag of functions used in multiple places.
 */
object Util {

  // lame attempt at improving readability
  type FileExtension = String
  type MimeType = String
  type MimeTypes = Map[FileExtension, MimeType]

  // from https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/util/mime_type.clj
  val defaultMimeTypes: MimeTypes = Map(
    "7z" -> "application/x-7z-compressed",
    "aac" -> "audio/aac",
    "ai" -> "application/postscript",
    "appcache" -> "text/cache-manifest",
    "asc" -> "text/plain",
    "atom" -> "application/atom+xml",
    "avi" -> "video/x-msvideo",
    "bin" -> "application/octet-stream",
    "bmp" -> "image/bmp",
    "bz2" -> "application/x-bzip",
    "class" -> "application/octet-stream",
    "cer" -> "application/pkix-cert",
    "crl" -> "application/pkix-crl",
    "crt" -> "application/x-x509-ca-cert",
    "css" -> "text/css",
    "csv" -> "text/csv",
    "deb" -> "application/x-deb",
    "dart" -> "application/dart",
    "dll" -> "application/octet-stream",
    "dmg" -> "application/octet-stream",
    "dms" -> "application/octet-stream",
    "doc" -> "application/msword",
    "dvi" -> "application/x-dvi",
    "edn" -> "application/edn",
    "eot" -> "application/vnd.ms-fontobject",
    "eps" -> "application/postscript",
    "etx" -> "text/x-setext",
    "exe" -> "application/octet-stream",
    "flv" -> "video/x-flv",
    "flac" -> "audio/flac",
    "gif" -> "image/gif",
    "gz" -> "application/gzip",
    "htm" -> "text/html",
    "html" -> "text/html",
    "ico" -> "image/x-icon",
    "iso" -> "application/x-iso9660-image",
    "jar" -> "application/java-archive",
    "jpe" -> "image/jpeg",
    "jpeg" -> "image/jpeg",
    "jpg" -> "image/jpeg",
    "js" -> "text/javascript",
    "json" -> "application/json",
    "lha" -> "application/octet-stream",
    "lzh" -> "application/octet-stream",
    "mov" -> "video/quicktime",
    "m4v" -> "video/mp4",
    "mp3" -> "audio/mpeg",
    "mp4" -> "video/mp4",
    "mpe" -> "video/mpeg",
    "mpeg" -> "video/mpeg",
    "mpg" -> "video/mpeg",
    "oga" -> "audio/ogg",
    "ogg" -> "audio/ogg",
    "ogv" -> "video/ogg",
    "pbm" -> "image/x-portable-bitmap",
    "pdf" -> "application/pdf",
    "pgm" -> "image/x-portable-graymap",
    "png" -> "image/png",
    "pnm" -> "image/x-portable-anymap",
    "ppm" -> "image/x-portable-pixmap",
    "ppt" -> "application/vnd.ms-powerpoint",
    "ps" -> "application/postscript",
    "qt" -> "video/quicktime",
    "rar" -> "application/x-rar-compressed",
    "ras" -> "image/x-cmu-raster",
    "rb" -> "text/plain",
    "rd" -> "text/plain",
    "rss" -> "application/rss+xml",
    "rtf" -> "application/rtf",
    "sgm" -> "text/sgml",
    "sgml" -> "text/sgml",
    "svg" -> "image/svg+xml",
    "swf" -> "application/x-shockwave-flash",
    "tar" -> "application/x-tar",
    "tif" -> "image/tiff",
    "tiff" -> "image/tiff",
    "ttf" -> "application/x-font-ttf",
    "txt" -> "text/plain",
    "webm" -> "video/webm",
    "wmv" -> "video/x-ms-wmv",
    "woff" -> "application/font-woff",
    "xbm" -> "image/x-xbitmap",
    "xls" -> "application/vnd.ms-excel",
    "xml" -> "text/xml",
    "xpm" -> "image/x-xpixmap",
    "xwd" -> "image/x-xwindowdump",
    "zip" -> "application/zip")

  val extRe = """\.([^./\\]+)$""".r.unanchored
  private def filenameExt(filename: String): Option[FileExtension] = {
    filename.toLowerCase match {
      case extRe(ext) => Some(ext)
      case _ => None
    }
  }

  /**
   * Return the mimetype for the given filename extension.
   *
   * @param filename The filename to look up the mimetype for.
   * @param overrides Optional overrides for the default ext->mimetype mapping.
   * @return The mimetype for filename extension ext, if any.
   */
  def mimeType(
      filename: String,
      overrides: MimeTypes = Map.empty): Option[MimeType] = {
    filenameExt(filename).flatMap { ext =>
      overrides.get(ext).orElse(defaultMimeTypes.get(ext))
    }
  }

  /**
   * Convert s to www-form-urlencoded format if possible.
   */
  def formEncodeString(s: String, encoding: Charset): Option[String] = {
    Try(URLEncoder.encode(s, encoding.toString)).toOption
  }

  def formEncodeString(s: String): Option[String] = formEncodeString(s, UTF_8)

  /**
   * Decode a www-form-urlencoded string, if possible.
   *
   * The primary way a string cannot be
   * decoded is if invalid percent-encoded hex digits appear, e.g. `%zz`.
   *
   * @param encoded a www-form-urlencoded string
   * @param encoding the encoding of `encoded`
   * @return The decoded string
   */
  def formDecodeString(encoded: String, encoding: Charset): Option[String] = {
    Try(URLDecoder.decode(encoded, encoding.toString)).toOption
  }

  def formDecodeString(encoded: String): Option[String] = formDecodeString(encoded, UTF_8)

  /**
   * Decode a www-form-urlencoded string to a Map. Useful for decoding form bodies and query strings.
   *
   * @param encoded a www-form-urlencoded string, presumably with `&` separating kvs, and `=`
   *                separating keys and values. e.g. `foo=bar&baz=quux`.
   * @param encoding The charset to use when decoding
   * @return A map of decoded names and values.  The values are all vectors.  A name with no value,
   *        e.g. `foo` in `a=b&foo` ends up with key `foo` and an empty Vector as its value.
   *        Repeated names, e.g. `x` in `x=1&x=2` ends up as key `x` with a vector of multiple
   *        values.  The typical case `a=b&c=d` decodes values as single element Vectors.
   *        Portions of `encoded` that can't be decoded into a map will be discarded.
   */
  def formDecodeMap(encoded: String, encoding: Charset = UTF_8): Map[String, Vector[String]] = {
    val init = Map.empty[String, Vector[String]]
    encoded.split("&").foldLeft(init) { (acc, x) =>
      val kv = x.split("=", 2).lift
      kv(0).filter(_.nonEmpty) match {
        case None => acc
        case Some(name) =>
          val value = kv(1).flatMap(formDecodeString(_, encoding)) match {
            case Some(v) => Vector(v)
            case None => Vector.empty
          }
          val updatedValue = acc.get(name) match {
            case Some(xs) => xs ++ value
            case None => value
          }
          acc.updated(name, updatedValue)
      }
    }
  }

  /**
   * Get an input stream for the string `s` in the given encoing.
   * @param s The string that should be made into an InputStream
   * @param encoding The encoding to use when converting the string to bytes.
   * @return An input stream returning the bytes of `s`.
   */
  def stringInputStream(s: String, encoding: Charset = UTF_8): InputStream = {
    new ByteArrayInputStream(s.getBytes(encoding))
  }

}
