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
CONCAT : '*';

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
    : list=(STRINGS|INTEGERS) id=idListObl                            #variablesCommand
    | FUNCTION LBRACE id=identifier RBRACE exp=stack                  #functionCommand
    | list=MACRO LBRACE id=identifier RBRACE LBRACE exp=STRING RBRACE #variablesCommand
    | READ                                                            #readCommand
    | EXECUTE LBRACE id=function RBRACE                               #executeCommand
    | ITERATE LBRACE id=function RBRACE                               #iterateCommand
    | REVERSE LBRACE id=function RBRACE                               #reverseCommand
    | ENTRY first=idListOpt second=idListOpt third=idListOpt          #entryCommand
    | SORT                                                            #sortCommand
    ;

identifier
	: IDENTIFIER
	;

idListObl
    : LBRACE identifier+ RBRACE
    ;

idListOpt
    : LBRACE identifier* RBRACE
    ;

function
	: operator=(LT | GT | EQUAL) #comparisonFunction
	| operator=(ADD | SUB)       #arithmeticFunction
	| ASSIGN                     #assignmentFunction
	| CONCAT                     #concatFunction
	| identifier                 #userFunction
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
