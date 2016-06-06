grammar Route;

// parser rules (start with lowercase)

route
  : (literal | param | wildcard)+
  ;

literal
  : LITERAL
  ;

param
  : PARAM
  ;

wildcard
  : '*'
  ;

fragment HEX
  : [a-fA-F0-9]
  ;

fragment PCT_ENC
  : '%' HEX HEX
  ;

// really "allowed in URL path", vaguely based on RFC-3986, definitely not
// correct, but good enough
LITERAL
  : ([a-zA-Z0-9-._~/] | PCT_ENC)+
  ;

// Extremely permissive identifiers / lazy unicode support
IDENTIFIER
  : ~[/:*{}\\ \t\r\n]+
  ;

PARAM
  : ':' IDENTIFIER  { setText(getText().substring(1)); } // trim leading ':'
  ;