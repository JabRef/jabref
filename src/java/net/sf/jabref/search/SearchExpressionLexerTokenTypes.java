// $ANTLR : "Lexer.g" -> "SearchExpressionLexer.java"$

package net.sf.jabref.search;

public interface SearchExpressionLexerTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_and = 4;
	int LITERAL_or = 5;
	int LITERAL_not = 6;
	int LITERAL_contains = 7;
	int LITERAL_matches = 8;
	int WS = 9;
	int LPAREN = 10;
	int RPAREN = 11;
	int EQUAL = 12;
	int EEQUAL = 13;
	int NEQUAL = 14;
	int QUOTE = 15;
	int STRING = 16;
	int LETTER = 17;
	int FIELDTYPE = 18;
}
