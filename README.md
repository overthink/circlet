# Circlet

Circlet is a Scala web application library inspired by Clojure's
[Ring](https://github.com/ring-clojure/ring). Circlet abstracts the details of
the underlying HTTP server and exposes a simple API for dealing with requests
and responses.  It's main feature is that it allows (forces) you to write your
applications as a function of type `HttpRequest => HttpResponse`.  Circlet is
named after Ring in a very clever and original way, with the added benefit of
creating confusion with the term Servlet in meatspace conversations.

## Project Goals

* Don't do too much
* Easy to use and understand the code
* Composable
* Type safe
* Fast enough

## Status

New and untested!

## Half-baked thoughts

* Writing of Circlet response bodies to servlet response could be a typeclass for extensibility
* Could just wrap HttpServletRequest, HttpServletResponse with a more Scala-like API rather 
  than copy data into Circlet's HttpRequest and HttpResponse classes...  ServletRequest even has
  getAttribute/setAttribute
   