package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

/** A CommonAST whose initialization copies hidden token
 *  information from the Token used to create a node.
 */
public class CommonASTWithHiddenTokens extends CommonAST {
    protected CommonHiddenStreamToken hiddenBefore, hiddenAfter; // references to hidden tokens
    
    public CommonASTWithHiddenTokens() {
	super();            
    }

    public CommonASTWithHiddenTokens(Token tok) {
	super(tok);
    }

    public CommonHiddenStreamToken getHiddenAfter() { return hiddenAfter; }

    public CommonHiddenStreamToken getHiddenBefore() { return hiddenBefore; }

    public void initialize(Token tok) {
	CommonHiddenStreamToken t = (CommonHiddenStreamToken)tok;
	super.initialize(t);
	hiddenBefore = t.getHiddenBefore();
	hiddenAfter  = t.getHiddenAfter();
    }
}
