package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.NoSuchElementException;

import antlr.collections.AST;
import antlr.collections.impl.BitSet;

public class TreeParser {
    /** The AST Null object; the parsing cursor is set to this when
     *  it is found to be null.  This way, we can test the
     *  token type of a node without having to have tests for null
     *  everywhere.
     */
    public static ASTNULLType ASTNULL = new ASTNULLType();

    /** Where did this rule leave off parsing; avoids a return parameter */
    protected AST _retTree;

    /** guessing nesting level; guessing==0 implies not guessing */
    // protected int guessing = 0;

    /** Nesting level of registered handlers */
    // protected int exceptionLevel = 0;

    protected TreeParserSharedInputState inputState;

    /** Table of token type to token names */
    protected String[] tokenNames;

    /** AST return value for a rule is squirreled away here */
    protected AST returnAST;

    /** AST support code; parser and treeparser delegate to this object */
    protected ASTFactory astFactory = new ASTFactory();

    /** Used to keep track of indentdepth for traceIn/Out */
    protected int traceDepth = 0;

    public TreeParser() {
        inputState = new TreeParserSharedInputState();
    }

    /** Get the AST return value squirreled away in the parser */
    public AST getAST() {
        return returnAST;
    }

    public ASTFactory getASTFactory() {
        return astFactory;
    }

    public String getTokenName(int num) {
        return tokenNames[num];
    }

    public String[] getTokenNames() {
        return tokenNames;
    }

    protected void match(AST t, int ttype) throws MismatchedTokenException {
        //System.out.println("match("+ttype+"); cursor is "+t);
        if (t == null || t == ASTNULL || t.getType() != ttype) {
            throw new MismatchedTokenException(getTokenNames(), t, ttype, false);
        }
    }

    /**Make sure current lookahead symbol matches the given set
     * Throw an exception upon mismatch, which is catch by either the
     * error handler or by the syntactic predicate.
     */
    public void match(AST t, BitSet b) throws MismatchedTokenException {
        if (t == null || t == ASTNULL || !b.member(t.getType())) {
            throw new MismatchedTokenException(getTokenNames(), t, b, false);
        }
    }

    protected void matchNot(AST t, int ttype) throws MismatchedTokenException {
        //System.out.println("match("+ttype+"); cursor is "+t);
        if (t == null || t == ASTNULL || t.getType() == ttype) {
            throw new MismatchedTokenException(getTokenNames(), t, ttype, true);
        }
    }

    /** @deprecated as of 2.7.2. This method calls System.exit() and writes
     *  directly to stderr, which is usually not appropriate when
     *  a parser is embedded into a larger application. Since the method is 
     *  <code>static</code>, it cannot be overridden to avoid these problems.
     *  ANTLR no longer uses this method internally or in generated code.
     */
    public static void panic() {
        System.err.println("TreeWalker: panic");
        System.exit(1);
    }

    /** Parser error-reporting function can be overridden in subclass */
    public void reportError(RecognitionException ex) {
        System.err.println(ex.toString());
    }

    /** Parser error-reporting function can be overridden in subclass */
    public void reportError(String s) {
        System.err.println("error: " + s);
    }

    /** Parser warning-reporting function can be overridden in subclass */
    public void reportWarning(String s) {
        System.err.println("warning: " + s);
    }

    /** Specify an object with support code (shared by
     *  Parser and TreeParser.  Normally, the programmer
     *  does not play with this, using setASTNodeType instead.
     */
    public void setASTFactory(ASTFactory f) {
        astFactory = f;
    }

    /** Specify the type of node to create during tree building.
     * 	@deprecated since 2.7.2
     */
    public void setASTNodeType(String nodeType) {
        setASTNodeClass(nodeType);
    }

    /** Specify the type of node to create during tree building */
    public void setASTNodeClass(String nodeType) {
        astFactory.setASTNodeType(nodeType);
    }

    public void traceIndent() {
        for (int i = 0; i < traceDepth; i++)
            System.out.print(" ");
    }

    public void traceIn(String rname, AST t) {
        traceDepth += 1;
        traceIndent();
        System.out.println("> " + rname +
                           "(" + (t != null?t.toString():"null") + ")" +
                           ((inputState.guessing > 0)?" [guessing]":""));
    }

    public void traceOut(String rname, AST t) {
        traceIndent();
        System.out.println("< " + rname +
                           "(" + (t != null?t.toString():"null") + ")" +
                           ((inputState.guessing > 0)?" [guessing]":""));
        traceDepth--;
    }
}
