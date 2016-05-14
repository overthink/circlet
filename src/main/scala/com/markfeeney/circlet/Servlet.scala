package com.markfeeney.circlet

import java.io.{InputStream, OutputStream, FileInputStream}
import java.security.cert.X509Certificate
import java.util.Locale
import collection.JavaConverters._

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

/**
 * Functionaliy for translating between Servlet world and Circlet world.
 */
object Servlet {

  /**
   * Copies headers out of servlet response and into a simple Map. Headers
   * with multiple values for the same name end up as a single comma separated
   * string in the returned map.  Header names are lowercased to make it
   * possible to reliably look them up later.
   */
  private def headers(request: HttpServletRequest): Map[String, String] = {
    request.getHeaderNames.asScala
      .foldLeft(Map.empty[String, String]) { case (acc, headerName) =>
        val name = headerName.toLowerCase(Locale.ENGLISH)
        acc.updated(name, request.getHeaders(headerName).asScala.mkString(","))
      }
  }

  private def sslClientCert(request: HttpServletRequest): Option[X509Certificate] = {
    val obj: AnyRef = request.getAttribute("javax.servlet.request.X509Certificate")
    // How do I know what type obj should be? http://stackoverflow.com/a/9913910/69689
    val certs = obj.asInstanceOf[Array[X509Certificate]]
    Option(certs).flatMap(_.headOption)
  }

  /** Convert a standard servlet request into a circlet HttpRequest. */
  def buildRequest(request: HttpServletRequest): Request = {
    Request(
      serverPort = request.getServerPort,
      serverName = request.getServerName,
      remoteAddr = request.getRemoteAddr,
      uri = request.getRequestURI,
      queryString = Option(request.getQueryString),
      scheme = Scheme.parse(request.getScheme),
      requestMethod = HttpMethod.parse(request.getMethod),
      protocol = request.getProtocol,
      headers = headers(request),
      sslClientCert = sslClientCert(request),
      body = Option(request.getInputStream)
    )
  }

  private def setHeaders(servletResponse: HttpServletResponse, headers: ResponseHeaders): Unit = {
    import ResponseHeaderValue._
    headers.foreach { case (k, v) =>
        v match {
          case Single(value) =>
            servletResponse.setHeader(k, value)
          case Multi(values) =>
            values.foreach { value =>
              servletResponse.addHeader(k, value)
            }
        }
    }
    // Some headers must be set through specific methods
    headers.get("Content-Type").foreach { value =>
      servletResponse.setContentType(value.asString)
    }
  }

  /**
   * Copy all bytes from `from` to `to`. Uses its own buffer. Doesn't close anything.
 *
   * @param from Source of bytes
   * @param to Destination of bytes
   */
  private def copy(from: InputStream, to: OutputStream): Unit = {
    // not idiomatic Scala; trying to avoid allocations
    val buffer = new Array[Byte](4096)
    var done = false
    while (!done) {
      val n = from.read(buffer)
      if (n > 0) {
        to.write(buffer, 0, n)
      } else {
        done = true
      }
    }
  }

  private def setBody(servletResponse: HttpServletResponse, body: ResponseBody): Unit = {
    import ResponseBody._
    body match {
      case StringBody(string) =>
        Cleanly(servletResponse.getWriter)(_.close()) { _.print(string) }
      case SeqBody(xs) =>
        Cleanly(servletResponse.getWriter)(_.close()) { writer =>
          xs.foreach { x => writer.print(x.toString) }
        }
      case StreamBody(inputStream) =>
        Cleanly(inputStream)(_.close()) { output =>
          copy(inputStream, servletResponse.getOutputStream)
        }
      case FileBody(file) =>
        Cleanly(new FileInputStream(file))(_.close()) { fis =>
          setBody(servletResponse, StreamBody(fis))
        }
    }
  }

  /**
   * Update the servlet resposne with relevant info from the Circlet response.
   *
   * @param servletResponse Might get updated
   * @param response Source of info that might get copied to servletResponse.
   */
  def updateServletResponse(servletResponse: HttpServletResponse, response: Response): Unit = {
    servletResponse.setStatus(response.status)
    setHeaders(servletResponse, response.headers)
    response.body.foreach { body =>
      setBody(servletResponse, body)
    }
  }
}
