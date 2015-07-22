/**
 * This is the antlr v4 grammar for defining search expressions.
 *
 * These search expressions are used for searching the bibtex library. They are heavily used for search groups.
 */
grammar Search;

WS: [ \t] -> skip; // whitespace is ignored/skipped

LPAREN:'(';
RPAREN:')';

EQUAL:'='; // semantically the same as CONTAINS
EEQUAL:'=='; // semantically the same as MATCHES
NEQUAL:'!=';

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
    LPAREN expression RPAREN                                  #parenExpression  // example: (author=miller)
    | left=expression operator=(AND | OR) right=expression    #binaryExpression // example: author = miller and title = test
    | NOT expression                                          #unaryExpression  // example: not author = miller
    | comparison                                              #atomExpression
    ;

comparison:
    left=name operator=(CONTAINS | MATCHES | EQUAL | EEQUAL | NEQUAL) right=name; // example: author != miller

name:
    STRING // example: "miller"
    | FIELDTYPE // example: author
    ;