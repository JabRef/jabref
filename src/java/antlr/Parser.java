package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import antlr.collections.impl.ASTArray;

/**A generic ANTLR parser (LL(k) for k>=1) containing a bunch of
 * utility routines useful at any lookahead depth.  We distinguish between
 * the LL(1) and LL(k) parsers because of efficiency.  This may not be
 * necessary in the near future.
 *
 * Each parser object contains the state of the parse including a lookahead
 * cache (the form of which is determined by the subclass), whether or
 * not the parser is in guess mode, where tokens come from, etc...
 *
 * <p>
 * During <b>guess</b> mode, the current lookahead token(s) and token type(s)
 * cache must be saved because the token stream may not have been informed
 * to save the token (via <tt>mark</tt>) before the <tt>try</tt> block.
 * Guessing is started by:
 * <ol>
 * <li>saving the lookahead cache.
 * <li>marking the current position in the TokenBuffer.
 * <li>increasing the guessing level.
 * </ol>
 *
 * After guessing, the parser state is restored by:
 * <ol>
 * <li>restoring the lookahead cache.
 * <li>rewinding the TokenBuffer.
 * <li>decreasing the guessing level.
 * </ol>
 *
 * @see antlr.Token
 * @see antlr.TokenBuffer
 * @see antlr.Tokenizer
 * @see antlr.LL1Parser
 * @see antlr.LLkParser
 */

import java.io.IOException;

import antlr.debug.MessageListener;
import antlr.debug.ParserListener;
import antlr.debug.ParserMatchListener;
import antlr.debug.ParserTokenListener;
import antlr.debug.SemanticPredicateListener;
import antlr.debug.SyntacticPredicateListener;
import antlr.debug.TraceListener;

public abstract class Parser {
    protected ParserSharedInputState inputState;

	/** Nesting level of registered handlers */
	// protected int exceptionLevel = 0;

    /** Table of token type to token names */
    protected String[] tokenNames;

    /** AST return value for a rule is squirreled away here */
    protected AST returnAST;

    /** AST support code; parser and treeparser delegate to this object */
    protected ASTFactory astFactory = new ASTFactory();

    private boolean ignoreInvalidDebugCalls = false;

	/** Used to keep track of indentdepth for traceIn/Out */
	protected int traceDepth = 0;

	public Parser() {
		inputState = new ParserSharedInputState();
	}

	public Parser(ParserSharedInputState state) {
		inputState = state;
	}

    public void addMessageListener(MessageListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addMessageListener() is only valid if parser built for debugging");
    }

    public void addParserListener(ParserListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addParserListener() is only valid if parser built for debugging");
    }

    public void addParserMatchListener(ParserMatchListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addParserMatchListener() is only valid if parser built for debugging");
    }

    public void addParserTokenListener(ParserTokenListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addParserTokenListener() is only valid if parser built for debugging");
    }

    public void addSemanticPredicateListener(SemanticPredicateListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addSemanticPredicateListener() is only valid if parser built for debugging");
    }

    public void addSyntacticPredicateListener(SyntacticPredicateListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addSyntacticPredicateListener() is only valid if parser built for debugging");
    }

    public void addTraceListener(TraceListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("addTraceListener() is only valid if parser built for debugging");
    }

    /**Get another token object from the token stream */
    public abstract void consume() throws TokenStreamException;
    /** Consume tokens until one matches the given token */
    public void consumeUntil(int tokenType) throws TokenStreamException {
	while (LA(1) != Token.EOF_TYPE && LA(1) != tokenType)
	    {
		consume();
	    }
    }
    /** Consume tokens until one matches the given token set */
    public void consumeUntil(BitSet set) throws TokenStreamException {
	while (LA(1) != Token.EOF_TYPE && !set.member(LA(1))) {
	    consume();
	}
    }
    protected void defaultDebuggingSetup(TokenStream lexer, TokenBuffer tokBuf) {
	// by default, do nothing -- we're not debugging
    }
    /** Get the AST return value squirreled away in the parser */
    public AST getAST() {
	return returnAST;
    }
    public ASTFactory getASTFactory() {
	return astFactory;
    }
    public String getFilename() {return inputState.filename;}

    public ParserSharedInputState getInputState() {
	return inputState;
    }

    public void setInputState(ParserSharedInputState state) {
	inputState = state;
    }

    public String getTokenName(int num) {
	return tokenNames[num];
    }
    public String[] getTokenNames() {
	return tokenNames;
    }
    public boolean isDebugMode() {return false;}
    /** Return the token type of the ith token of lookahead where i=1
	 * is the current token being examined by the parser (i.e., it
	 * has not been matched yet).
	 */
    public abstract int LA(int i) throws TokenStreamException;
    /**Return the ith token of lookahead */
    public abstract Token LT(int i) throws TokenStreamException;
    // Forwarded to TokenBuffer
    public int mark() {
	return inputState.input.mark();
    }
    /**Make sure current lookahead symbol matches token type <tt>t</tt>.
	 * Throw an exception upon mismatch, which is catch by either the
	 * error handler or by the syntactic predicate.
	 */
    public void match(int t) throws MismatchedTokenException, TokenStreamException {
		 if ( LA(1)!=t )
			 throw new MismatchedTokenException(tokenNames, LT(1), t, false, getFilename());
		 else
			 // mark token as consumed -- fetch next token deferred until LA/LT
			 consume();
    }
    /**Make sure current lookahead symbol matches the given set
	 * Throw an exception upon mismatch, which is catch by either the
	 * error handler or by the syntactic predicate.
	 */
	public void match(BitSet b) throws MismatchedTokenException, TokenStreamException {
		if ( !b.member(LA(1)) )
			throw new MismatchedTokenException(tokenNames, LT(1), b, false, getFilename());
		else
			// mark token as consumed -- fetch next token deferred until LA/LT
			consume();
	}
    public void matchNot(int t) throws MismatchedTokenException, TokenStreamException {
	if ( LA(1)==t )
	    // Throws inverted-sense exception
	    throw new MismatchedTokenException(tokenNames, LT(1), t, true, getFilename());
	else
	    // mark token as consumed -- fetch next token deferred until LA/LT
	    consume();
    }
    public static void panic() {
	System.err.println("Parser: panic");
	System.exit(1);
    }
    public void removeMessageListener(MessageListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("removeMessageListener() is only valid if parser built for debugging");
    }
    public void removeParserListener(ParserListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("removeParserListener() is only valid if parser built for debugging");
    }
    public void removeParserMatchListener(ParserMatchListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("removeParserMatchListener() is only valid if parser built for debugging");
    }
    public void removeParserTokenListener(ParserTokenListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("removeParserTokenListener() is only valid if parser built for debugging");
    }
    public void removeSemanticPredicateListener(SemanticPredicateListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("removeSemanticPredicateListener() is only valid if parser built for debugging");
    }
    public void removeSyntacticPredicateListener(SyntacticPredicateListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new IllegalArgumentException("removeSyntacticPredicateListener() is only valid if parser built for debugging");
    }
    public void removeTraceListener(TraceListener l) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("removeTraceListener() is only valid if parser built for debugging");
    }

    /** Parser error-reporting function can be overridden in subclass */
    public void reportError(RecognitionException ex) {
	System.err.println(ex);
    }

    /** Parser error-reporting function can be overridden in subclass */
    public void reportError(String s) {
	if ( getFilename()==null ) {
	    System.err.println("error: " + s);
	}
	else {
	    System.err.println(getFilename()+": error: " + s);
	}
    }

    /** Parser warning-reporting function can be overridden in subclass */
    public void reportWarning(String s) {
	if ( getFilename()==null ) {
	    System.err.println("warning: "+s);
	}
	else {
	    System.err.println(getFilename()+": warning: " + s);
	}
    }

    public void rewind(int pos) {
	inputState.input.rewind(pos);
    }
    /** Specify an object with support code (shared by
	 *  Parser and TreeParser.  Normally, the programmer
	 *  does not play with this, using setASTNodeType instead.
	 */
    public void setASTFactory(ASTFactory f) {
	astFactory = f;
    }
    public void setASTNodeClass(String cl) {
	astFactory.setASTNodeType(cl);
    }
    /** Specify the type of node to create during tree building; use setASTNodeClass now
 *  to be consistent with Token Object Type accessor.
 */
    public void setASTNodeType (String nodeType) {
	setASTNodeClass(nodeType);
    }
    public void setDebugMode(boolean debugMode) {
	if (!ignoreInvalidDebugCalls)
	    throw new RuntimeException("setDebugMode() only valid if parser built for debugging");
    }
    public void setFilename(String f) {inputState.filename=f;}
    public void setIgnoreInvalidDebugCalls(boolean value) {
	ignoreInvalidDebugCalls = value;
    }
    /** Set or change the input token buffer */
    public void setTokenBuffer(TokenBuffer t) { inputState.input = t; }

	public void traceIndent() {
		for( int i = 0; i < traceDepth; i++ )
			System.out.print(" ");
	}
	public void traceIn(String rname) throws TokenStreamException {
		traceDepth += 1;
		traceIndent();
		System.out.println("> "+rname+"; LA(1)=="+LT(1).getText()+
								 ((inputState.guessing>0)?" [guessing]":""));
	}
	public void traceOut(String rname) throws TokenStreamException {
		traceIndent();
		System.out.println("< "+rname+"; LA(1)=="+LT(1).getText()+
								 ((inputState.guessing>0)?" [guessing]":""));
		traceDepth -= 1;
	}
}
