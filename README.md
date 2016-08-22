# Circlet

[![Build Status](https://travis-ci.org/overthink/circlet.svg?branch=master)](https://travis-ci.org/overthink/circlet)

```scala
import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.{JettyAdapter, Response}

val app = handler { req => Response(body = "Hello world!") }
JettyAdapter.run(app)
```

Circlet is a simple Scala web application library.  Circlet allows you to
write your application as a plain function of type `Request =>
Option[Response]` (also called a `Handler`).

Composable middleware functions of type `Handler => Handler` are used to add
reusable functionality to handlers.  Circlet includes some useful middleware
like parameter parsing (including multipart, i.e. file upload), and cookie
handling. For routing and higher-level functionality, see
[Usher](https://github.com/overthink/usher).

Circlet attempts to abstract away the underlying web server, but it's somewhat
theoretical, since only Jetty is currently supported.

Circlet also supports websockets, [see below](#websockets).

Circlet aims to be in roughly the same space as other web server interfaces
like [WSGI](https://wsgi.readthedocs.io/en/latest/),
[Rack](http://rack.github.io/), [WAI](https://github.com/yesodweb/wai), and
[Ring](https://github.com/ring-clojure/ring).  In fact, Ring is the inspiration for
Circlet, and large parts of Circlet are ported directly from it.

## Try it

Latest release:

```scala
libraryDependencies += "com.markfeeney" % "circlet_2.11" % "0.2.0"
```

There is also an [example circlet project](https://github.com/overthink/circlet-example) 
showing how to get a running Circlet app.

## Project Goals

In priority order:

1. Small: few classes, few dependencies, no special tooling
1. Composable
1. Maintainable code
1. Type safe enough
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
// respond is the continuation fn
val cpsHelloWorld: Handler = request => respond => {
  val resp = Response(body = "hello world")
  respond(resp)
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
val streamingHandler: Handler = req => respond => {
  val conn = // get a db connection
  try {
    val resp = Response(body = SeqBody(makeLazySeq(req, conn)))
    respond(resp)
  } finally {
    conn.close() // response has been sent, close connection
  }
}
```

Circlet's CPS approach is borrowed from
[WAI](https://hackage.haskell.org/package/wai-3.2.1/docs/Network-Wai.html)
(thanks [stebulus](https://github.com/stebulus)).

## <a name="websockets"></a>Websockets

As of 0.2.0, Criclet supports websockets.  These are somewhat bolted on,
unfortunately: they're Jetty-specific and do not take part in middleware.  Regular handlers still
behave as normal, but a separate mapping of paths to websocket implementations
is provided to the server at creation time.  Here is a simple example that
returns whatever it is sent, converted to uppercase.

```scala
import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.{JettyAdapter, JettyOptions, Response}
import com.markfeeney.circlet.JettyWebSocket

val upper = JettyWebSocket(
  onText = (session, msg) => session.getRemote.sendString(msg.toUpperCase)
)
val opts = JettyOptions(webSockets = Map("/upper/" -> upper))
val app = handler { req => Response(body = "websocket example, connect to ws://upper/") }
JettyAdapter.run(app, opts)
```

## TODO

* Better understand websocket threading model
* Async handlers? (not yet convinced that I care)
* Make websockets use [JSR 356](https://jcp.org/en/jsr/detail?id=356) rather than Jetty-specific classes
* Support other web servers besides Jetty?

## Known issues

* A CPS handler could call the continuation function multiple times.  I'm not
  sure how to prevent this, (or if I should) so don't do it.  Unless you need
  to.

## Other options

There are a lot of Scala web libraries and frameworks out there.  Here are
some that I feel are close in spirit to circlet.

* [http4s](http://http4s.org/) - very similar to circlet, focus on async, heavy use of scalaz
* [finagle](https://twitter.github.io/finagle/) - Twitter's big system for building high-concurrency servers

## License                                                                                                                                                                            
                                                                                                                                                                                      
Copyright &copy; 2016 Mark Feeney
                                                                                                                                                     
Released under the MIT license.
