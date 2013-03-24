header {
package net.sf.jabref.search;
}

class SearchExpressionLexer extends Lexer;

options {
	k = 2;
	exportVocab = SearchExpressionLexer;
	caseSensitive = false;
	caseSensitiveLiterals = false;
	charVocabulary = '\3'..'\377'; // 8 bit
	testLiterals = false;
}

tokens {
	"and";
	"or";
	"not";
	"contains";
	"matches";

}



WS options { paraphrase = "white space"; }
	:	(' '
	|
	'\t'
	)
		{ $setType(Token.SKIP); }
	;

LPAREN options { paraphrase = "'('"; }
	:	'('
	;

RPAREN options { paraphrase = "')'"; }
	:	')'
	;

EQUAL options { paraphrase = "'='"; }
	:	"=";

EEQUAL options { paraphrase = "'=='"; }
	:	"==";

NEQUAL options { paraphrase = "'!='"; }
	:	"!=";

QUOTE options { paraphrase = "'\"'"; }
	:	'"';

STRING options { paraphrase = "a text literal"; }
	:	QUOTE! (~'"')* QUOTE!;

protected
LETTER options { paraphrase = "a letter"; testLiterals = true; }
	: ~(' ' | '\t' | '"' | '(' | ')' | '=' | '!'); //'a'..'z';

FIELDTYPE options { paraphrase = "a field type"; testLiterals = true; }
	:	( LETTER )+
	;

