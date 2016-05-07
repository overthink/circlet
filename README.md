# Circlet

Circlet is a Scala web application library heavily inspired by Clojure's
[Ring](https://github.com/ring-clojure/ring). It's main feature is that it 
allows (forces) you to write your applications as a function of 
type `HttpRequest => HttpResponse` rather than deal with Servlet or other more
complicated web frameworks.

Circlet is named after Ring in a very clever and original way, with the added 
benefit of creating confusion with the term Servlet in meatspace conversations.

## Project Goals

(In priority order.)

1. Don't do too much
1. Easy to use and understand the code
1. Composable
1. Type safe
1. Fast enough

## Status

Unfinished, some unit tests, untested (and largely unusable still) in production!

## Design note

Presently, the web server interface expects handlers (and thus middleware) to be implemented in 
continuation-passing style ([CPS](https://en.wikipedia.org/wiki/Continuation-passing_style)). e.g.

```scala
// this
val cpsHelloWorld: CpsHandler = (request, k) => {
  val resp = Response(body = "hello world")
  k(resp)
}

// instead of
val helloWorld: Handler = request => {
  Response(body = "hello world")
}
```

This is kind of a pain for simple cases, so I've provided [some conversions](src/main/scala/com/markfeeney/circlet/CpsConverters.scala) 
to automatically convert simple handlers and middleware to their CPS counterparts. This means you 
can largeley ignore CPS interface and write simple handlers and middleware unless you need fancy
resource cleanup.

My rationale for using CPS is to allow handlers to clean up any resources they've allocated before the 
request is completely out of Circlet's control.  In particular, this is to support streaming responses. e.g.
 
```scala
val streamingHandler: CpsHandler = (req, k) => {
  val conn = // get a db connection or some expensive resource needing cleanup
  try {
    val resp = Response(body = makeLazySeq(req, conn))
    k(resp)
  } finally {
    conn.close()
  }
}
```

Ring has a pretty [cool way of achiving streaming](https://github.com/ring-clojure/ring/blob/d302502ea4da392016963d33bd81028bc761d8c8/ring-core/src/ring/util/io.clj#L26-L29), 
but it involves an additional thread (which I don't love).  Haskell's [WAI](https://hackage.haskell.org/package/wai-3.2.1/docs/Network-Wai.html)
(thanks [stebulus](https://github.com/stebulus)) uses CPS to 
allow handlers to clean up without an additional thread, so I thought I'd give that a try. CPS 
significantly complicates things (IMO), however, so I'm not sure if I'll keep this.  It's nice to 
have the option but how commonly is it actually needed? TBD.

I am definietly no expert here, so I probably haven't thought of/realized many things. Please 
beware -- or better yet -- educate me.

## Other half-baked thoughts

* Writing of Circlet response bodies to servlet response could be a typeclass for extensibility
* Could just wrap HttpServletRequest, HttpServletResponse with a more Scala-like API rather 
  than copy data into Circlet's HttpRequest and HttpResponse classes...  ServletRequest even has
  getAttribute/setAttribute (meh)
