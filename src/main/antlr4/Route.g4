grammar Route;

// parser rules (start with lowercase)

route : ('/' pathElem)+ ;

pathElem : literal
         | wildcard
         | param
         ;

literal: Literal ;
wildcard : '*';
param : ':' Literal ;

// lexer rules (start with uppercase)

fragment IdentifierChar : [a-zA-Z0-9-_] ;
fragment HexDigit : [a-fA-F0-9] ;
fragment PercentEncoded : '%' HexDigit HexDigit ;
Literal : (IdentifierChar | PercentEncoded)+ ;
