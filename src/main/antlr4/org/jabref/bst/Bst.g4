grammar Bst;

// Lexer

STRINGS : 'STRINGS';
INTEGERS : 'INTEGERS';
FUNCTION : 'FUNCTION';
EXECUTE : 'EXECUTE';
SORT : 'SORT';
ITERATE : 'ITERATE';
REVERSE : 'REVERSE';
ENTRY : 'ENTRY';
READ : 'READ';
MACRO : 'MACRO';

LBRACE : '{';
RBRACE : '}';
GT : '>';
LT : '<';
EQUAL : '=';
ASSIGN : ':=';
ADD : '+';
SUB : '-';
MUL : '*';

fragment LETTER : ('a'..'z'|'A'..'Z'|'.'|'$');
fragment DIGIT : [0-9];

IDENTIFIER : LETTER (LETTER|DIGIT|'_')*;
INTEGER : '#' ('+'|'-')? DIGIT+;
QUOTED : '\'' IDENTIFIER;
STRING : '"' (~('"'))* '"';

WS: [ \r\n\t]+ -> skip;
LINE_COMMENT : '%' ~('\n'|'\r')* '\r'? '\n' -> skip;

// Parser

start
    : commands+ EOF
    ;

commands
    : STRINGS idList
    | INTEGERS idList
    | FUNCTION id stack
    | MACRO id
    | READ
    | EXECUTE LBRACE function RBRACE
    | ITERATE LBRACE function RBRACE
    | REVERSE LBRACE function RBRACE
    | ENTRY idList0 idList0 idList0
    | SORT
    ;


identifier
    : IDENTIFIER
    ;

id  : LBRACE identifier RBRACE
    ;

idList
    : LBRACE IDENTIFIER+ RBRACE
    ;

idList0
    : LBRACE IDENTIFIER* RBRACE
    ;

function
	: LT | GT | EQUAL | ADD | SUB | ASSIGN | MUL
	| identifier
	;

stack
	: LBRACE stackitem+ RBRACE
	;

stackitem
	: function
	| STRING
	| INTEGER
	| QUOTED
	| stack
	;
