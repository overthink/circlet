package com.markfeeney.poise

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import com.markfeeney.poise.parser.RouteParser.{RouteContext, WildcardContext, ParamContext}
import com.markfeeney.poise.parser.{RouteBaseListener, RouteParser, RouteLexer}
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.{CommonTokenStream, RecognitionException, Recognizer, BaseErrorListener, ANTLRInputStream}

trait Route {
  // e.g. "/foo/:fooId/bar/:barId"
  def path: String
}

// Based on clout: https://github.com/weavejester/clout
object Route {

  sealed trait ParamValue {
    def value: String
  }
  final case class Single(value: String) extends ParamValue
  final case class Multiple(xs: Vector[String]) {
    assert(xs.nonEmpty, "Multiple must contain at least one value")
    def value: String = xs.head
  }

  case class Impl private(
    path: String,
    paramNames: Vector[String],
    regex: Regex) extends Route

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
    println(tree.toStringTree(parser)) // useful debug
    tree
  }

  // Extract param names from parse tree (params are things like :foo)
  // Wildcards end up as a param named "*"
  private def paramNames(parseTree: RouteContext): Vector[String] = {
    val params: ListBuffer[String] = ListBuffer.empty
    val listener = new RouteBaseListener {
      override def enterParam(ctx: ParamContext): Unit = {
        params.append(ctx.getText)
      }
      override def enterWildcard(ctx: WildcardContext): Unit = {
        params.append(ctx.getText)
      }
    }
    ParseTreeWalker.DEFAULT.walk(listener, parseTree)
    params.toVector
  }

  // - parse path into route AST
  // - extract param names
  // - generate and compile regex
  def compile(path: String): Route = {
    val parseTree = parseRoute(path)
    val params = paramNames(parseTree)
    // walk the parse tree and pull out the params we find (in order of appearance)
    Impl(path, params, "".r)
  }

  // Use route's compiled regex to match url
  // associate param names with whatever regex captured
  // return as map
  def parse(route: Route, url: String): Map[String, ParamValue] = {
    Map.empty
  }

}
