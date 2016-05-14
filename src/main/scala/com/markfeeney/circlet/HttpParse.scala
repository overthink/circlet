package com.markfeeney.circlet

import scala.util.matching.Regex

/**
 * Helper regexs for parsing HTTP.
 * see https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/util/parsing.clj
 */
object HttpParse {

  // HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068
  val reToken: Regex = "[!#$%&'*\\-+.0-9A-Z\\^_`a-z\\|~]+".r

  // HTTP quoted-string: <"> *<any TEXT except "> <">. See RFC2068.
  val reQuoted: Regex = "\"(\\\"|[^\"])*\"".r

  // HTTP value: token | quoted-string. See RFC2109
  val reValue: Regex = (reToken.toString + "|" + reQuoted.toString).r

}
