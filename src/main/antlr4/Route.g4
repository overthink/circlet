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

// anything not part of a param or wildcard
LITERAL
  : ~[:*{}\\]+
  ;

IDENTIFIER
  : [a-zA-Z0-9-_]+
  ;

PARAM
  : ':' IDENTIFIER  { setText(getText().substring(1)); } // trim leading ':'
  ;