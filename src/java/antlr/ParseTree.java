package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

import antlr.*;
import antlr.collections.AST;

public abstract class ParseTree extends BaseAST {

	/** Walk parse tree and return requested number of derivation steps.
	 *  If steps <= 0, return node text.  If steps == 1, return derivation
	 *  string at step.
	 */
	public String getLeftmostDerivationStep(int step) {
        if ( step<=0 ) {
			return toString();
		}
		StringBuffer buf = new StringBuffer(2000);
        getLeftmostDerivation(buf, step);
		return buf.toString();
	}

	public String getLeftmostDerivation(int maxSteps) {
		StringBuffer buf = new StringBuffer(2000);
		buf.append("    "+this.toString());
		buf.append("\n");
		for (int d=1; d<maxSteps; d++) {
			buf.append(" =>");
			buf.append(getLeftmostDerivationStep(d));
			buf.append("\n");
		}
		return buf.toString();
	}

	/** Get derivation and return how many you did (less than requested for
	 *  subtree roots.
	 */
	protected abstract int getLeftmostDerivation(StringBuffer buf, int step);

	// just satisfy BaseAST interface; unused as we manually create nodes

	public void initialize(int i, String s) {
	}
	public void initialize(AST ast) {
	}
	public void initialize(Token token) {
	}
}
