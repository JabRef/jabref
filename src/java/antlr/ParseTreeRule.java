package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

import antlr.Token;
import antlr.collections.AST;

public class ParseTreeRule extends ParseTree {
	public static final int INVALID_ALT = -1;

	protected String ruleName;
	protected int altNumber;  // unused until I modify antlr to record this

	public ParseTreeRule(String ruleName) {
		this(ruleName,INVALID_ALT);
	}

	public ParseTreeRule(String ruleName, int altNumber) {
		this.ruleName = ruleName;
		this.altNumber = altNumber;
	}

	public String getRuleName() {
		return ruleName;
	}

	/** Do a step-first walk, building up a buffer of tokens until
	 *  you've reached a particular step and print out any rule subroots
	 *  insteads of descending.
	 */
	protected int getLeftmostDerivation(StringBuffer buf, int step) {
		int numReplacements = 0;
		if ( step<=0 ) {
			buf.append(' ');
			buf.append(toString());
			return numReplacements;
		}
		AST child = getFirstChild();
		numReplacements = 1;
		// walk child printing them out, descending into at most one
		while ( child!=null ) {
			if ( numReplacements>=step || child instanceof ParseTreeToken ) {
				buf.append(' ');
				buf.append(child.toString());
			}
			else {
				// descend for at least one more derivation; update count
				int remainingReplacements = step-numReplacements;
				int n = ((ParseTree)child).getLeftmostDerivation(buf,
																 remainingReplacements);
				numReplacements += n;
			}
			child = child.getNextSibling();
		}
		return numReplacements;
	}

	public String toString() {
		if ( altNumber==INVALID_ALT ) {
			return '<'+ruleName+'>';
		}
		else {
			return '<'+ruleName+"["+altNumber+"]>";
		}
	}
}
