package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

/** This token knows what index 0..n-1 it is from beginning of stream.
 *  Designed to work with TokenStreamRewriteEngine.java
 */
public class TokenWithIndex extends CommonToken {
    /** Index into token array indicating position in input stream */
    int index;

    public TokenWithIndex() {
	super();
    }

    public TokenWithIndex(int i, String t) {
	super(i,t);
    }

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return "["+index+":\"" + getText() + "\",<" + getType() + ">,line=" + line + ",col=" +
col + "]\n";
	}
}
