package antlr.debug;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

import antlr.*;
import antlr.collections.impl.BitSet;

import java.util.Stack;

/** Override the standard matching and rule entry/exit routines
 *  to build parse trees.  This class is useful for 2.7.3 where
 *  you can specify a superclass like
 *
 *   class TinyCParser extends Parser(ParseTreeDebugParser);
 */
public class ParseTreeDebugParser extends LLkParser {
	/** Each new rule invocation must have it's own subtree.  Tokens
	 *  are added to the current root so we must have a stack of subtree roots.
	 */
	protected Stack currentParseTreeRoot = new Stack();

	/** Track most recently created parse subtree so that when parsing
	 *  is finished, we can get to the root.
	 */
	protected ParseTreeRule mostRecentParseTreeRoot = null;

	/** For every rule replacement with a production, we bump up count. */
	protected int numberOfDerivationSteps = 1; // n replacements plus step 0

	public ParseTreeDebugParser(int k_) {
		super(k_);
	}

	public ParseTreeDebugParser(ParserSharedInputState state, int k_) {
		super(state,k_);
	}

	public ParseTreeDebugParser(TokenBuffer tokenBuf, int k_) {
		super(tokenBuf, k_);
	}

	public ParseTreeDebugParser(TokenStream lexer, int k_) {
		super(lexer,k_);
	}

	public ParseTree getParseTree() {
		return mostRecentParseTreeRoot;
	}

	public int getNumberOfDerivationSteps() {
		return numberOfDerivationSteps;
	}

	public void match(int i) throws MismatchedTokenException, TokenStreamException {
		addCurrentTokenToParseTree();
		super.match(i);
	}

	public void match(BitSet bitSet) throws MismatchedTokenException, TokenStreamException {
		addCurrentTokenToParseTree();
		super.match(bitSet);
	}

	public void matchNot(int i) throws MismatchedTokenException, TokenStreamException {
		addCurrentTokenToParseTree();
		super.matchNot(i);
	}

	/** This adds LT(1) to the current parse subtree.  Note that the match()
	 *  routines add the node before checking for correct match.  This means
	 *  that, upon mismatched token, there will a token node in the tree
	 *  corresponding to where that token was expected.  For no viable
	 *  alternative errors, no node will be in the tree as nothing was
	 *  matched() (the lookahead failed to predict an alternative).
	 */
	protected void addCurrentTokenToParseTree() throws TokenStreamException {
		if (inputState.guessing>0) {
			return;
		}
		ParseTreeRule root = (ParseTreeRule)currentParseTreeRoot.peek();
		ParseTreeToken tokenNode = null;
		if ( LA(1)==Token.EOF_TYPE ) {
			tokenNode = new ParseTreeToken(new antlr.CommonToken("EOF"));
		}
		else {
			tokenNode = new ParseTreeToken(LT(1));
		}
		root.addChild(tokenNode);
	}

	/** Create a rule node, add to current tree, and make it current root */
	public void traceIn(String s) throws TokenStreamException {
		if (inputState.guessing>0) {
			return;
		}
		ParseTreeRule subRoot = new ParseTreeRule(s);
		if ( currentParseTreeRoot.size()>0 ) {
			ParseTreeRule oldRoot = (ParseTreeRule)currentParseTreeRoot.peek();
			oldRoot.addChild(subRoot);
		}
		currentParseTreeRoot.push(subRoot);
		numberOfDerivationSteps++;
	}

	/** Pop current root; back to adding to old root */
	public void traceOut(String s) throws TokenStreamException {
		if (inputState.guessing>0) {
			return;
		}
		mostRecentParseTreeRoot = (ParseTreeRule)currentParseTreeRoot.pop();
	}

}
