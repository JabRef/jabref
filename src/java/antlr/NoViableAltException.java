package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.AST;

public class NoViableAltException extends RecognitionException {
	public Token token;
	public AST node;	// handles parsing and treeparsing

	public NoViableAltException(AST t) {
		super("NoViableAlt");
		node = t;
		fileName = "<AST>";
	}

	public NoViableAltException(Token t, String fileName) {
		super("NoViableAlt");
		token = t;
		line = t.getLine();
		column = t.getColumn();
		this.fileName = fileName;
	}

	/**
	 * @deprecated As of ANTLR 2.7.0
	 */
	public String getErrorMessage () {
		return getMessage();
	}

	/**
	 * Returns a clean error message (no line number/column information)
	 */
	public String getMessage ()
	{
		if (token != null) {
			return "unexpected token: "+token.getText();
		}

		// must a tree parser error if token==null
		if ( node==TreeParser.ASTNULL ) {
			return "unexpected end of subtree";
		}
		return "unexpected AST node: "+node.toString();
	}

    /**
     * Returns a string representation of this exception.
     */
    public String toString() {
	if ( token!=null ) { // AST or Token?
	    return FileLineFormatter.getFormatter().getFormatString(fileName,line)+getMessage();
	}
	return getMessage();
    }
}
