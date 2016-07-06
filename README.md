# Circlet

[![Build Status](https://travis-ci.org/overthink/circlet.svg?branch=master)](https://travis-ci.org/overthink/circlet)

```scala
val app = handler { req => Response(body = "Hello world!") }
JettyAdapter.run(app)
```

Circlet is a Scala web application library heavily inspired by the brilliant
[Ring](https://github.com/ring-clojure/ring) from the Clojure world.  Large
parts of Circlet are directly ported from Ring.

Circlet's main feature is that it allows (forces) you to write your
application as a function of type `Request => Response`.  It also abstracts
away the details of the underlying web server, though presently only Jetty is
supported.

As with Ring, middleware functions are used for adding reusable bits of
functionality to applications. Circlet includes some useful middleware like
parameter parsing (including multipart), and cookie handling.  For routing and
higher-level functionality, see [Usher](https://github.com/overhink/usher).

## Try it

Latest release:

```scala
libraryDependencies ++= Seq(
  "com.markfeeney" % "circlet_2.11" % "0.1.0"
)
```

Next version snapshots:

```scala
resolvers += Opts.resolver.sonatypeSnapshots
libraryDependencies ++= Seq(
  "com.markfeeney" % "circlet_2.11" % "0.2.0-SNAPSHOT"
)
```

There is also an [example circlet project](https://github.com/overthink/circlet-example) 
showing how to get a running Circlet app.

## Project Goals

In priority order:

1. Lightweight, small surface area
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

e.g. So you need to write this:

```scala
val cpsHelloWorld: Handler = request => k => {
  val resp = Response(body = "hello world")
  k(resp)
}
```

instead of something like this:

```scala
// unfortunately doesn't compile
val helloWorld: Handler = request => {
  Response(body = "hello world")
}
```

This is a pain in the neck in many cases, so
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

This CPS approach is borrowed from Haskell's
[WAI](https://hackage.haskell.org/package/wai-3.2.1/docs/Network-Wai.html)
(thanks [stebulus](https://github.com/stebulus)).

## TODO

* Middleware
  * ~~regular params (query string, post body)~~
  * ~~cookies~~
  * ~~multipart params (i.e. file upload)~~
  * ~~sessions~~ (deferred)
  * serve static resources/files
* Async handlers
* Actual adapter abstraction (right now only `JettyAdapter` and `JettyOptions` exist)

## Known issues

* A CPS handler could call the continuation function multiple times.  I'm not
  sure how to prevent this, (or if I should) so don't do it.  Unless you need
  to.
* CPS is nice, but in theory it can easily blow the stack since nothing really
  returns.  I'm hoping this won't be a problem in practice since most apps
  won't compose more than a few tens of middleware and handlers, and
  presumably user code running inside handlers won't use CPS.  But I'm not
  sure, so the whole thing might be doomed :)

## License                                                                                                                                                                            
                                                                                                                                                                                      
Copyright &copy; 2016 Mark Feeney
                                                                                                                                                     
Released under the MIT license.
