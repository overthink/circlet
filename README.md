# Circlet

[![Build Status](https://travis-ci.org/overthink/circlet.svg?branch=master)](https://travis-ci.org/overthink/circlet)

```scala
import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.{JettyAdapter, Response}

val app = handler { req => Response(body = "Hello world!") }
JettyAdapter.run(app)
```

Circlet is a simple Scala web application library.  Circlet abstracts away the
underlying web server and allows you to write your application as a plain function
of type `Request => Option[Response]` (also called a `Handler`).

Composable middleware functions of type `Handler => Handler` are used to add
reusable functionality to handlers.  Circlet includes some useful middleware
like parameter parsing (including multipart, i.e. file upload), and cookie
handling. For routing and higher-level functionality, see
[Usher](https://github.com/overthink/usher).

Circlet aims to be in roughly the same space as other web server interfaces
like [WSGI](https://wsgi.readthedocs.io/en/latest/),
[Rack](http://rack.github.io/), [WAI](https://github.com/yesodweb/wai), and
[Ring](https://github.com/ring-clojure/ring).  In fact, Ring is the inspiration for
Circlet, and large parts of Circlet are ported directly from Ring.

## Try it

Latest release:

```scala
libraryDependencies += "com.markfeeney" % "circlet_2.11" % "0.1.0"
```

There is also an [example circlet project](https://github.com/overthink/circlet-example) 
showing how to get a running Circlet app.

## Project Goals

In priority order:

1. Small: few core classes, no special tooling required
1. Composable
1. Maintainable code
1. Type safe
1. Fast enough

## Design

Handlers and middleware must be implemented in continuation-passing 
style ([CPS](https://en.wikipedia.org/wiki/Continuation-passing_style)).

```scala
type Cont = Option[Response] => Sent.type
type Handler = Request => Cont => Sent.type
type Middleware = Handler => Handler
```

i.e. you need to write this:

```scala
// k is the continuation fn
val cpsHelloWorld: Handler = request => k => {
  val resp = Response(body = "hello world")
  k(resp)
}
```

instead of something like this:

```scala
// doesn't compile
val helloWorld: Handler = request => {
  Response(body = "hello world")
}
```

Worrying about the continuation can be a pain in the neck sometimes, so
[helpers](src/main/scala/com/markfeeney/circlet/Circlet.scala#L23) are
available for cases where you don't need the extra features CPS affords. e.g.

```scala
val helloWorld = handler { req => Response(body = "hello world") }
```

I've gone the CPS route so handlers can allocate "request scoped" resources
and properly clean them up when request processing is complete.  As an
example, Circlet's multipart parameter middleware uses this to [clean up temp
files](src/main/scala/com/markfeeney/circlet/middleware/MultipartParams.scala#L163-L167)
created when handling file uploads.

Here's another (contrived) example: streaming a response from the database.

```scala
val streamingHandler: Handler = req => k => {
  val conn = // get a db connection
  try {
    val resp = Response(body = SeqBody(makeLazySeq(req, conn)))
    k(resp)
  } finally {
    conn.close() // response has been sent, close connection
  }
}
```

Circlet's CPS approach is borrowed from
[WAI](https://hackage.haskell.org/package/wai-3.2.1/docs/Network-Wai.html)
(thanks [stebulus](https://github.com/stebulus)).

## TODO

* Async handlers
* Middleware
  * ~~regular params (query string, post body)~~
  * ~~cookies~~
  * ~~multipart params (i.e. file upload)~~
  * ~~sessions~~ (deferred)
  * serve static resources/files
* Separate Jetty-specific bits into separate project
* Support other web servers besides Jetty

## Known issues

* A CPS handler could call the continuation function multiple times.  I'm not
  sure how to prevent this, (or if I should) so don't do it.  Unless you need
  to.

## License                                                                                                                                                                            
                                                                                                                                                                                      
Copyright &copy; 2016 Mark Feeney
                                                                                                                                                     
Released under the MIT license.
