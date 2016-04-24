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

Unfinished, untested!

## Half-baked thoughts

* Writing of Circlet response bodies to servlet response could be a typeclass for extensibility
* Could just wrap HttpServletRequest, HttpServletResponse with a more Scala-like API rather 
  than copy data into Circlet's HttpRequest and HttpResponse classes...  ServletRequest even has
  getAttribute/setAttribute
