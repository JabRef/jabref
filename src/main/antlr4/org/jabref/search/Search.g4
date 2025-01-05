/**
 * This is the antlr v4 grammar for defining search expressions.
 *
 * These search expressions are used for searching the bibtex library. They are heavily used for search groups.
 */
grammar Search;
options { caseInsensitive = true; }

WS: [ \t\n\r]+ -> skip; // whitespace is ignored/skipped

LPAREN: '(';
RPAREN: ')';

EQUAL: '='; // case insensitive contains, semantically the same as CONTAINS
CEQUAL: '=!'; // case sensitive contains

EEQUAL: '=='; // exact match case insensitive, semantically the same as MATCHES
CEEQUAL: '==!'; // exact match case sensitive

REQUAL: '=~'; // regex check case insensitive
CREEQUAL: '=~!'; // regex check case sensitive

NEQUAL: '!='; //  negated case insensitive contains
NCEQUAL: '!=!'; // negated case sensitive contains

NEEQUAL: '!=='; // negated case insensitive exact match
NCEEQUAL: '!==!'; // negated case sensitive exact match

NREQUAL: '!=~'; // negated regex check case insensitive
NCREEQUAL: '!=~!'; // negated regex check case sensitive

AND: 'AND';
OR: 'OR';
CONTAINS: 'CONTAINS';
MATCHES: 'MATCHES';
NOT: 'NOT';

FIELD: [A-Z]+;
STRING_LITERAL: '"' ('\\"' | ~["])* '"';    // " should be escaped with a backslash
TERM: ('\\' [=!~()] | ~[ \t\n\r=!~()])+;    // =!~() should be escaped with a backslash

start
    : EOF
    | andExpression EOF
    ;

andExpression
    : expression+                                         #implicitAndExpression   // example: author = miller year = 2010 --> equivalent to: author = miller AND year = 2010
    ;

expression
    : LPAREN andExpression RPAREN                         #parenExpression        // example: (author = miller)
    | NOT expression                                      #negatedExpression      // example: NOT author = miller
    | left = expression bin_op = AND right = expression   #binaryExpression       // example: author = miller AND year = 2010
    | left = expression bin_op = OR right = expression    #binaryExpression       // example: author = miller OR year = 2010
    | comparison                                          #comparisonExpression   // example: miller OR author = miller
    ;

comparison
    : FIELD operator searchValue    // example: author = miller
    | searchValue                   // example: miller
    ;

operator
    : EQUAL
    | CEQUAL
    | EEQUAL
    | CEEQUAL
    | REQUAL
    | CREEQUAL
    | NEQUAL
    | NCEQUAL
    | NEEQUAL
    | NCEEQUAL
    | NREQUAL
    | NCREEQUAL
    | CONTAINS
    | MATCHES
    ;

searchValue
    : STRING_LITERAL
    | FIELD
    | TERM
    ;
