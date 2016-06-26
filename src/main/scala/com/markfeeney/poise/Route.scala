package com.markfeeney.poise
/** Route matching mostly ported from clout: https://github.com/weavejester/clout */

import com.markfeeney.circlet.Request

import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import com.markfeeney.poise.parser.RouteParser.{LiteralContext, ParamContext, RouteContext, WildcardContext}
import com.markfeeney.poise.parser.{RouteBaseListener, RouteLexer, RouteParser}
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.{ANTLRInputStream, BaseErrorListener, CommonTokenStream, RecognitionException, Recognizer}

import scala.util.Try

/**
 * Ideally you get one of these via `Route.compile()`.
 * @param path e.g. "/foo/:id/bar/:name/&#42;/:quux"
 * @param paramNames e.g., in above, Vector("id", "name", "&#42;", "quux")
 * @param regex A regular expression to extract values from a url pased on `path` and `paramNames`.
 */
case class Route(path: String, paramNames: Vector[String], regex: Regex)

object Route {

  sealed trait ParamValue {
    def value: String
  }
  final case class Single(value: String) extends ParamValue
  final case class Multiple(xs: Vector[String]) extends ParamValue {
    assert(xs.nonEmpty, "Multiple must contain at least one value")
    def value: String = xs.head
  }
  object ParamValue {
    implicit def str2Single(s: String): Single = Single(s)
    implicit def vec2Multiple(vec: Vector[String]): Multiple = Multiple(vec)
  }

  case class Params(params: Map[String, ParamValue])

  // Why'd I think ANTLR was a good idea again? http://stackoverflow.com/a/26573239/69689
  // This thing is used to make ANTLR throw a decent exception when it runs into trouble.
  private object ThrowingErrorListener extends BaseErrorListener {
    override def syntaxError(
      recognizer: Recognizer[_, _],
      offendingSymbol: Any,
      line: Int,
      charPositionInLine: Int,
      msg: String,
      e: RecognitionException): Unit = {
      throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg)
    }
  }

  // Return parse tree for path, which is something like "/foo/:id/bar/:barId/*/whatev"
  private def parseRoute(path: String): RouteContext = {
    val input = new ANTLRInputStream(path)
    val lexer = new RouteLexer(input)
    lexer.removeErrorListeners()
    lexer.addErrorListener(ThrowingErrorListener) // throw a reasonable exception on failed lexing
    val tokens = new CommonTokenStream(lexer)
    val parser = new RouteParser(tokens)
    parser.removeErrorListeners()
    parser.addErrorListener(ThrowingErrorListener) // throw a reasonable exception on failed parsing
    val tree = parser.route()
    // println(tree.toStringTree(parser)) // useful debug
    tree
  }

  // Extract param names from parse tree (params are things like :foo)
  // Wildcards end up as a param named "*"
  private def paramNames(parseTree: RouteContext): Vector[String] = {
    val params: ListBuffer[String] = ListBuffer.empty
    val listener = new RouteBaseListener {
      override def enterParam(ctx: ParamContext): Unit = params.append(ctx.getText)
      override def enterWildcard(ctx: WildcardContext): Unit = params.append(ctx.getText)
    }
    ParseTreeWalker.DEFAULT.walk(listener, parseTree)
    params.toVector
  }

  // walk the parse tree and replace each parameter or wildcard with a regex that
  // will capture that part of a URL.
  private def buildRegex(parseTree: RouteContext): Regex = {
    val str = new StringBuilder("^")
    val listener = new RouteBaseListener {
      override def enterLiteral(ctx: LiteralContext): Unit = str.append(ctx.LITERAL())
      override def enterParam(ctx: ParamContext): Unit = str.append("([^/?]+)")
      override def enterWildcard(ctx: WildcardContext): Unit = str.append("(.*?)")
    }
    ParseTreeWalker.DEFAULT.walk(listener, parseTree)
    str.append("$")
    str.r
  }

  /**
    * Parse `path` into a Route. Throws if `path` cannot be parsed. (Based on the assumption that most
    * of the time routes will not be built dynamically, and you'd rather just get the exception ASAP.)
    *
    * @param path Something like "/foo/:foo/&#42;/bar/:bar"
    * @return A Route object that can be used to extract params from candidate URLs that match it.
    */
  def compile(path: String): Route = {
    val parseTree = parseRoute(path)
    val params = paramNames(parseTree)
    val regex = buildRegex(parseTree)
    // println(path)
    // println(regex)
    Route(path, params, regex)
  }

  /**
    * If possible, parse param values from `url` based on `route`.
    *
    * @param route A compiled Route object
    * @param url A candidate URL like "/foo/42/blah/bar/9000"
    * @return Map of values extracted from `url`, or None if url doesn't match route.
    */
  def parse(route: Route, url: String): Option[Map[String, ParamValue]] = {
    route.regex.findFirstMatchIn(url).map { m =>
      route.paramNames
        .zipWithIndex
        .map { case (param, i) => param -> m.group(i + 1) }
        .foldLeft(Map[String, ParamValue]()) { case (acc, (k, v)) =>
          acc.get(k) match {
            case Some(Single(oldV)) => acc.updated(k, Vector(oldV, v))
            case Some(Multiple(vs)) => acc.updated(k, vs :+ v)
            case _ => acc.updated(k, v)
          }
        }
    }
  }

  /** Return parsed route params stored on `req`, if any. */
  def get(req: Request): Option[Params] = {
    Try(req.attrs("routeParams").asInstanceOf[Params]).toOption
  }

  /** Set `params` to be the route params on `req`. */
  def set(req: Request, params: Params): Request = {
    req.updated("routeParams", params)
  }

  implicit def str2Route(routeSpec: String): Route = Route.compile(routeSpec)

}
