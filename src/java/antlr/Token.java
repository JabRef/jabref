package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

/** A token is minimally a token type.  Subclasses can add the text matched
 *  for the token and line info. 
 */
public class Token implements Cloneable {
	// constants
	public static final int MIN_USER_TYPE = 4;
	public static final int NULL_TREE_LOOKAHEAD = 3;
	public static final int INVALID_TYPE = 0;
	public static final int EOF_TYPE = 1;
	public static final int SKIP = -1;

	// each Token has at least a token type
	int type=INVALID_TYPE;
	
	// the illegal token object
	public static Token badToken = new Token(INVALID_TYPE, "<no text>");

	public Token() {;}
	public Token(int t) { type = t; }
	public Token(int t, String txt) { type = t; setText(txt); }
	public int getColumn() { return 0; }
	public int getLine() { return 0; }
	public String getText() { return "<no text>"; }
	public int getType() { return type; }
	public void setColumn(int c) {;}
	public void setLine(int l) {;}
	public void setText(String t) {;}
	public void setType(int t) { type = t; }
	public String toString() {
		return "[\""+getText()+"\",<"+type+">]";
	}
}
