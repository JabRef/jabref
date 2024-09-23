/**
 * This is the antlr v4 grammar for defining search expressions.
 *
 * These search expressions are used for searching the bibtex library. They are heavily used for search groups.
 */
grammar Search;

WS: [ \t] -> skip; // whitespace is ignored/skipped

LPAREN:'(';
RPAREN:')';

EQUAL:'='; // case insensitive contains, semantically the same as CONTAINS
CEQUAL:'=!'; // case sensitive contains

EEQUAL:'=='; // exact match case insensitive, semantically the same as MATCHES

NEQUAL:'!='; //  negated case insensitive contains
NCEQUAL:'!=!'; // negated case sensitive contains

REQUAL:'=~'; // regex check case insensitive
CREEQUAL:'=~!'; // regex check case sensitive

NREQUAL:'!=~'; // negated regex check case insensitive
NCREEQUAL:'!=~!'; // negated regex check case sensitive

AND:[aA][nN][dD]; // 'and' case insensitive
OR:[oO][rR]; // 'or' case insensitive
CONTAINS:[cC][oO][nN][tT][aA][iI][nN][sS]; // 'contains' case insensitive
MATCHES:[mM][aA][tT][cC][hH][eE][sS]; // 'matches' case insensitive
NOT:[nN][oO][tT]; // 'not' case insensitive

STRING:QUOTE (~'"')* QUOTE;
QUOTE:'"';

FIELDTYPE:LETTER+;
// fragments are not accessible from the code, they are only for describing the grammar better
fragment LETTER : ~[ \t"()=!];


start:
    expression EOF;

// labels are used to refer to parts of the rules in the generated code later on
// label=actualThingy
expression:
    LPAREN expression RPAREN                         #parenExpression  // example: (author=miller)
    | NOT expression                                 #unaryExpression  // example: not author = miller
    | left=expression operator=AND right=expression  #binaryExpression // example: author = miller and title = test
    | left=expression operator=OR right=expression   #binaryExpression // example: author = miller or title = test
    | comparison                                     #atomExpression
    ;

comparison:
    left=name operator=(CONTAINS | EQUAL | CEQUAL | MATCHES | EEQUAL | NEQUAL | NCEQUAL | REQUAL | CREEQUAL | NREQUAL | NCREEQUAL) right=name // example: author != miller
    | right=name                                                                 // example: miller (search all fields)
    ;

name:
    STRING // example: "miller"
    | FIELDTYPE // example: author
    ;
