package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

import antlr.Token;
import antlr.collections.AST;

public class ParseTreeToken extends ParseTree {
	protected Token token;

	public ParseTreeToken(Token token) {
		this.token = token;
	}

	protected int getLeftmostDerivation(StringBuffer buf, int step) {
		buf.append(' ');
		buf.append(toString());
		return step; // did on replacements
	}

	public String toString() {
		if ( token!=null ) {
			return token.getText();
		}
		return "<missing token>";
	}
}
