// $ANTLR : "preproc.g" -> "Preprocessor.java"$

package antlr.preprocessor;

public interface PreprocessorTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_tokens = 4;
	int HEADER_ACTION = 5;
	int SUBRULE_BLOCK = 6;
	int ACTION = 7;
	int LITERAL_class = 8;
	int ID = 9;
	int LITERAL_extends = 10;
	int SEMI = 11;
	int TOKENS_SPEC = 12;
	int OPTIONS_START = 13;
	int ASSIGN_RHS = 14;
	int RCURLY = 15;
	int LITERAL_protected = 16;
	int LITERAL_private = 17;
	int LITERAL_public = 18;
	int BANG = 19;
	int ARG_ACTION = 20;
	int LITERAL_returns = 21;
	int RULE_BLOCK = 22;
	int LITERAL_throws = 23;
	int COMMA = 24;
	int LITERAL_exception = 25;
	int LITERAL_catch = 26;
	int ALT = 27;
	int ELEMENT = 28;
	int LPAREN = 29;
	int RPAREN = 30;
	int ID_OR_KEYWORD = 31;
	int CURLY_BLOCK_SCARF = 32;
	int WS = 33;
	int NEWLINE = 34;
	int COMMENT = 35;
	int SL_COMMENT = 36;
	int ML_COMMENT = 37;
	int CHAR_LITERAL = 38;
	int STRING_LITERAL = 39;
	int ESC = 40;
	int DIGIT = 41;
	int XDIGIT = 42;
}
