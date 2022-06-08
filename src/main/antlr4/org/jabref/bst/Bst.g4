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

GT : '>';
LT : '<';
EQUAL : '=';
ASSIGN : ':=';
ADD : '+';
SUB : '-';
CONCAT : '*';
IF : 'if$';
WHILE : 'while$';
LBRACE : '{';
RBRACE : '}';

fragment LETTER : ('a'..'z'|'A'..'Z'|'.'|'$');
fragment DIGIT : [0-9];

IDENTIFIER : LETTER (LETTER|DIGIT|'_')*;
INTEGER : '#' ('+'|'-')? DIGIT+;
QUOTED : '\'' IDENTIFIER;
STRING : '"' (~('"'))* '"';

WS: [ \r\n\t]+ -> skip;
LINE_COMMENT : '%' ~('\n'|'\r')* '\r'? '\n' -> skip;

// Parser

bstFile
    : commands+ EOF
    ;

commands
    : STRINGS ids=idListObl                                           #stringsCommand
    | INTEGERS ids=idListObl                                          #integersCommand
    | FUNCTION LBRACE id=identifier RBRACE function=stack             #functionCommand
    | MACRO LBRACE id=identifier RBRACE LBRACE repl=STRING RBRACE     #macroCommand
    | READ                                                            #readCommand
    | EXECUTE LBRACE bstFunction RBRACE                               #executeCommand
    | ITERATE LBRACE bstFunction RBRACE                               #iterateCommand
    | REVERSE LBRACE bstFunction RBRACE                               #reverseCommand
    | ENTRY idListOpt idListOpt idListOpt                             #entryCommand
    | SORT                                                            #sortCommand
    ;

identifier
	: IDENTIFIER
	;

// Obligatory identifier list
idListObl
    : LBRACE identifier+ RBRACE
    ;

// Optional identifier list
idListOpt
    : LBRACE identifier* RBRACE
    ;

bstFunction
	: LT | GT | EQUAL | ADD | SUB | ASSIGN | CONCAT
	| identifier
	;

stack
	: LBRACE stackitem+ RBRACE
	;

stackitem
	: bstFunction
	| STRING
	| INTEGER
	| QUOTED
	| stack
	;
