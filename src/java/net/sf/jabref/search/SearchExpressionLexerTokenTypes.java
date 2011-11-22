/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
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
