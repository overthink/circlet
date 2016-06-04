# Circlet

Circlet is a Scala web application library heavily inspired by Clojure's
[Ring](https://github.com/ring-clojure/ring).  If we're truthful, it's basically a port of Ring.
Its main feature is that it allows (forces) you to write your application as a simple function
from `Request` to `Response`.  It also hides the details of the underlying web server 
(which is always Jetty at the moment).

As with Ring, middleware functions are used for adding reusable bits of functionality to 
applications. Circlet includes some useful middleware like parameter parsing, 
and file upload handling.

Circlet is named after Ring in a very clever and original way.

## Project Goals

(In priority order.)

1. Don't do too much
1. Easy to use and understand the code
1. Composable
1. Type safe
1. Fast enough

## TODO

To get to a somewhat useful v1, I think I need the following done:

* Middleware
  * ~~regular params (query string, post body)~~
  * cookies
  * ~~multipart params (i.e. file upload)~~
  * sessions
  * serve static resources/files

## Design

Presently, the web server interface expects handlers (and thus middleware) to be implemented in 
continuation-passing style ([CPS](https://en.wikipedia.org/wiki/Continuation-passing_style)).

```scala
// important types
type CpsHandler = (Request, Response => Done.type) => Done.type
type Handler = Request => Response
```

e.g. You need to write this:

```scala
val cpsHelloWorld: CpsHandler = (request, k) => {
  val resp = Response(body = "hello world")
  k(resp)
}
```

instead of this:

```scala
val helloWorld: Handler = request => {
  Response(body = "hello world")
}
```

I've done this so handlers can allocate "request scoped" resources and properly clean them up when request
processing is complete.  As an example, Circlet's multipart parameter middleware uses this to 
[clean up temp files](src/main/scala/com/markfeeney/circlet/middleware/MultipartParams.scala#L163-L167) created
when handling file uploads.  Here's another (contrived) example: streaming a response from the database.

```scala
val streamingHandler: CpsHandler = (req, k) => {
  val conn = // get a db connection or some expensive resource needing cleanup
  try {
    val resp = Response(body = SeqBody(makeLazySeq(req, conn)))
    k(resp)
  } finally {
    conn.close()
  }
}
```

All this CPS business is borrowed directly from Haskell's [WAI](https://hackage.haskell.org/package/wai-3.2.1/docs/Network-Wai.html)
(thanks [stebulus](https://github.com/stebulus)), which I should probably study more.

Finally, sometimes CPS is a pain for simple handlers, so I've provided some [conversions](src/main/scala/com/markfeeney/circlet/CpsConverters.scala) 
to automatically convert non-CPS handlers and middleware to their CPS counterparts.  TBD if this will prove
useful or not.

## Known issues

* A CPS handler could call he continuation function multiple times.  I'm not sure how to prevent this, so don't do it.
