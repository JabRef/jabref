package antlr.debug;

import antlr.ParserSharedInputState;
import antlr.TokenStreamException;
import antlr.LLkParser;
import antlr.TokenBuffer;
import antlr.TokenStream;
import antlr.MismatchedTokenException;
import antlr.RecognitionException;
import antlr.collections.impl.BitSet;
import java.io.IOException;
import antlr.TokenStreamException;

import antlr.debug.ParserEventSupport;

import java.lang.reflect.Constructor;

public class LLkDebuggingParser extends LLkParser implements DebuggingParser {
	protected ParserEventSupport parserEventSupport = new ParserEventSupport(this);

	private boolean _notDebugMode = false;
	protected String ruleNames[];
	protected String semPredNames[];


	public LLkDebuggingParser(int k_) {
		super(k_);
	}
	public LLkDebuggingParser(ParserSharedInputState state, int k_) {
		super(state, k_);
	}
	public LLkDebuggingParser(TokenBuffer tokenBuf, int k_) {
		super(tokenBuf, k_);
	}
	public LLkDebuggingParser(TokenStream lexer, int k_) {
		super(lexer, k_);
	}
	public void addMessageListener(MessageListener l) {
		parserEventSupport.addMessageListener(l);
	}
	public void addParserListener(ParserListener l) {
		parserEventSupport.addParserListener(l);
	}
	public void addParserMatchListener(ParserMatchListener l) {
		parserEventSupport.addParserMatchListener(l);
	}
	public void addParserTokenListener(ParserTokenListener l) {
		parserEventSupport.addParserTokenListener(l);
	}
	public void addSemanticPredicateListener(SemanticPredicateListener l) {
		parserEventSupport.addSemanticPredicateListener(l);
	}
	public void addSyntacticPredicateListener(SyntacticPredicateListener l) {
		parserEventSupport.addSyntacticPredicateListener(l);
	}
	public void addTraceListener(TraceListener l) {
		parserEventSupport.addTraceListener(l);
	}
	/**Get another token object from the token stream */
	public void consume() {
		int la_1 = -99;
		try {la_1 = LA(1);}	
		catch (TokenStreamException ignoreAnException) {}
		super.consume();
		parserEventSupport.fireConsume(la_1);
	}
	protected void fireEnterRule(int num,int data) {
		if (isDebugMode())
			parserEventSupport.fireEnterRule(num,inputState.guessing,data);
	}
	protected void fireExitRule(int num,int data) {
		if (isDebugMode())
			parserEventSupport.fireExitRule(num,inputState.guessing,data);
	}
	protected boolean fireSemanticPredicateEvaluated(int type, int num, boolean condition) {
		if (isDebugMode())
			return parserEventSupport.fireSemanticPredicateEvaluated(type,num,condition,inputState.guessing);
		else
			return condition;
	}
	protected void fireSyntacticPredicateFailed() {
		if (isDebugMode())
			parserEventSupport.fireSyntacticPredicateFailed(inputState.guessing);
	}
	protected void fireSyntacticPredicateStarted() {
		if (isDebugMode())
			parserEventSupport.fireSyntacticPredicateStarted(inputState.guessing);
	}
	protected void fireSyntacticPredicateSucceeded() {
		if (isDebugMode())
			parserEventSupport.fireSyntacticPredicateSucceeded(inputState.guessing);
	}
	public String getRuleName(int num) {
		return ruleNames[num];
	}
	public String getSemPredName(int num) {
		return semPredNames[num];
	}
	public synchronized void goToSleep() {
		try {wait();}
		catch (InterruptedException e) {	}		
	}
	public boolean isDebugMode() {
		return !_notDebugMode;
	}
	public boolean isGuessing() {
		return inputState.guessing > 0;
	}
	/** Return the token type of the ith token of lookahead where i=1
	 * is the current token being examined by the parser (i.e., it
	 * has not been matched yet).
	 */
	public int LA(int i) throws TokenStreamException {
		int la = super.LA(i);
		parserEventSupport.fireLA(i, la);
		return la;
	}
	/**Make sure current lookahead symbol matches token type <tt>t</tt>.
	 * Throw an exception upon mismatch, which is catch by either the
	 * error handler or by the syntactic predicate.
	 */
	public void match(int t) throws MismatchedTokenException, TokenStreamException {
		String text = LT(1).getText();
		int la_1 = LA(1);
		try {
			super.match(t);
			parserEventSupport.fireMatch(t, text, inputState.guessing);
		}
		catch (MismatchedTokenException e) {
			if (inputState.guessing == 0)
				parserEventSupport.fireMismatch(la_1, t, text, inputState.guessing);
			throw e;
		}
	}
	/**Make sure current lookahead symbol matches the given set
	 * Throw an exception upon mismatch, which is catch by either the
	 * error handler or by the syntactic predicate.
	 */
	public void match(BitSet b) throws MismatchedTokenException, TokenStreamException {
		String text = LT(1).getText();
		int la_1 = LA(1);
		try {
			super.match(b);
			parserEventSupport.fireMatch(la_1,b, text, inputState.guessing);
		}
		catch (MismatchedTokenException e) {
			if (inputState.guessing == 0)
				parserEventSupport.fireMismatch(la_1, b, text, inputState.guessing);
			throw e;
		}
	}
	public void matchNot(int t) throws MismatchedTokenException, TokenStreamException {
		String text = LT(1).getText();
		int la_1 = LA(1);
		try {
			super.matchNot(t);
			parserEventSupport.fireMatchNot(la_1, t, text, inputState.guessing);
		}
		catch (MismatchedTokenException e) {
			if (inputState.guessing == 0)
				parserEventSupport.fireMismatchNot(la_1, t, text, inputState.guessing);
			throw e;
		}
	}
	public void removeMessageListener(MessageListener l) {
		parserEventSupport.removeMessageListener(l);
	}
	public void removeParserListener(ParserListener l) {
		parserEventSupport.removeParserListener(l);
	}
	public void removeParserMatchListener(ParserMatchListener l) {
		parserEventSupport.removeParserMatchListener(l);
	}
	public void removeParserTokenListener(ParserTokenListener l) {
		parserEventSupport.removeParserTokenListener(l);
	}
	public void removeSemanticPredicateListener(SemanticPredicateListener l) {
		parserEventSupport.removeSemanticPredicateListener(l);
	}
	public void removeSyntacticPredicateListener(SyntacticPredicateListener l) {
		parserEventSupport.removeSyntacticPredicateListener(l);
	}
	public void removeTraceListener(TraceListener l) {	
		parserEventSupport.removeTraceListener(l);
	}
	/** Parser error-reporting function can be overridden in subclass */
	public void reportError(RecognitionException ex) {
		parserEventSupport.fireReportError(ex);
		super.reportError(ex);
	}
	/** Parser error-reporting function can be overridden in subclass */
	public void reportError(String s) {
		parserEventSupport.fireReportError(s);
		super.reportError(s);
	}
	/** Parser warning-reporting function can be overridden in subclass */
	public void reportWarning(String s) {
		parserEventSupport.fireReportWarning(s);
		super.reportWarning(s);
	}
	public void setDebugMode(boolean value) {
		_notDebugMode = !value;
	}
	public void setupDebugging(TokenBuffer tokenBuf) {
		setupDebugging(null, tokenBuf);
	}
	public void setupDebugging(TokenStream lexer) {
		setupDebugging(lexer, null);
	}
	/** User can override to do their own debugging */
	protected void setupDebugging(TokenStream lexer, TokenBuffer tokenBuf) {
		setDebugMode(true);
		// default parser debug setup is ParseView
		try {
			try {
				Class.forName("javax.swing.JButton");
			}
			catch (ClassNotFoundException e) {
				System.err.println("Swing is required to use ParseView, but is not present in your CLASSPATH");
				System.exit(1);
			}
			Class c = Class.forName("antlr.parseview.ParseView");
			Constructor constructor = c.getConstructor(new Class[] {LLkDebuggingParser.class, TokenStream.class, TokenBuffer.class});
			constructor.newInstance(new Object[] {this, lexer, tokenBuf});
		}
		catch(Exception e) {
			System.err.println("Error initializing ParseView: "+e);
			System.err.println("Please report this to Scott Stanchfield, thetick@magelang.com");
			System.exit(1);
		}
	}
	public synchronized void wakeUp() {
		notify();
	}
}
