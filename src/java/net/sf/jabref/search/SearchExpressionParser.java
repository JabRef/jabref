// $ANTLR 2.7.1: "Parser.g" -> "SearchExpressionParser.java"$

package net.sf.jabref.search;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;

public class SearchExpressionParser extends antlr.LLkParser
       implements SearchExpressionParserTokenTypes
 {

	public boolean caseSensitive = false;

protected SearchExpressionParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public SearchExpressionParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected SearchExpressionParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public SearchExpressionParser(TokenStream lexer) {
  this(lexer,3);
}

public SearchExpressionParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

	public final void quotedRegularExpression(
		boolean caseSensitive
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST quotedRegularExpression_AST = null;
		Token  var_s = null;
		AST var_s_AST = null;
		
		var_s = LT(1);
		if (inputState.guessing==0) {
			var_s_AST = (AST)astFactory.create(var_s);
			astFactory.addASTChild(currentAST, var_s_AST);
		}
		match(STRING);
		if ( inputState.guessing==0 ) {
			quotedRegularExpression_AST = (AST)currentAST.root;
			
							quotedRegularExpression_AST = astFactory.make((new ASTArray(2)).add(new RegExNode(RegularExpression,var_s.getText(),caseSensitive)).add(quotedRegularExpression_AST));
						
			currentAST.root = quotedRegularExpression_AST;
			currentAST.child = quotedRegularExpression_AST!=null &&quotedRegularExpression_AST.getFirstChild()!=null ?
				quotedRegularExpression_AST.getFirstChild() : quotedRegularExpression_AST;
			currentAST.advanceChildToEnd();
		}
		quotedRegularExpression_AST = (AST)currentAST.root;
		returnAST = quotedRegularExpression_AST;
	}
	
	public final void simpleRegularExpression(
		boolean caseSensitive
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST simpleRegularExpression_AST = null;
		Token  var_s = null;
		AST var_s_AST = null;
		
		var_s = LT(1);
		if (inputState.guessing==0) {
			var_s_AST = (AST)astFactory.create(var_s);
			astFactory.addASTChild(currentAST, var_s_AST);
		}
		match(FIELDTYPE);
		if ( inputState.guessing==0 ) {
			simpleRegularExpression_AST = (AST)currentAST.root;
			
							simpleRegularExpression_AST = astFactory.make((new ASTArray(2)).add(new RegExNode(RegularExpression,var_s.getText(),caseSensitive)).add(simpleRegularExpression_AST));
						
			currentAST.root = simpleRegularExpression_AST;
			currentAST.child = simpleRegularExpression_AST!=null &&simpleRegularExpression_AST.getFirstChild()!=null ?
				simpleRegularExpression_AST.getFirstChild() : simpleRegularExpression_AST;
			currentAST.advanceChildToEnd();
		}
		simpleRegularExpression_AST = (AST)currentAST.root;
		returnAST = simpleRegularExpression_AST;
	}
	
	public final void searchExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST searchExpression_AST = null;
		
		condition();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		AST tmp1_AST = null;
		if (inputState.guessing==0) {
			tmp1_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp1_AST);
		}
		match(Token.EOF_TYPE);
		searchExpression_AST = (AST)currentAST.root;
		returnAST = searchExpression_AST;
	}
	
	public final void condition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST condition_AST = null;
		
		boolean synPredMatched6 = false;
		if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
			int _m6 = mark();
			synPredMatched6 = true;
			inputState.guessing++;
			try {
				{
				expression();
				match(LITERAL_and);
				condition();
				}
			}
			catch (RecognitionException pe) {
				synPredMatched6 = false;
			}
			rewind(_m6);
			inputState.guessing--;
		}
		if ( synPredMatched6 ) {
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp2_AST = null;
			tmp2_AST = (AST)astFactory.create(LT(1));
			match(LITERAL_and);
			condition();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			if ( inputState.guessing==0 ) {
				condition_AST = (AST)currentAST.root;
				condition_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(And)).add(condition_AST));
				currentAST.root = condition_AST;
				currentAST.child = condition_AST!=null &&condition_AST.getFirstChild()!=null ?
					condition_AST.getFirstChild() : condition_AST;
				currentAST.advanceChildToEnd();
			}
			condition_AST = (AST)currentAST.root;
		}
		else {
			boolean synPredMatched8 = false;
			if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
				int _m8 = mark();
				synPredMatched8 = true;
				inputState.guessing++;
				try {
					{
					expression();
					match(LITERAL_or);
					condition();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched8 = false;
				}
				rewind(_m8);
				inputState.guessing--;
			}
			if ( synPredMatched8 ) {
				expression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				AST tmp3_AST = null;
				tmp3_AST = (AST)astFactory.create(LT(1));
				match(LITERAL_or);
				condition();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				if ( inputState.guessing==0 ) {
					condition_AST = (AST)currentAST.root;
					condition_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(Or)).add(condition_AST));
					currentAST.root = condition_AST;
					currentAST.child = condition_AST!=null &&condition_AST.getFirstChild()!=null ?
						condition_AST.getFirstChild() : condition_AST;
					currentAST.advanceChildToEnd();
				}
				condition_AST = (AST)currentAST.root;
			}
			else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3)))) {
				expression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				condition_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			returnAST = condition_AST;
		}
		
	public final void expression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expression_AST = null;
		
		switch ( LA(1)) {
		case STRING:
		case FIELDTYPE:
		{
			expressionSearch();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			expression_AST = (AST)currentAST.root;
			break;
		}
		case LPAREN:
		{
			AST tmp4_AST = null;
			tmp4_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			condition();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp5_AST = null;
			tmp5_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			expression_AST = (AST)currentAST.root;
			break;
		}
		default:
			if ((LA(1)==LITERAL_not) && (LA(2)==STRING||LA(2)==FIELDTYPE)) {
				AST tmp6_AST = null;
				tmp6_AST = (AST)astFactory.create(LT(1));
				match(LITERAL_not);
				expressionSearch();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				if ( inputState.guessing==0 ) {
					expression_AST = (AST)currentAST.root;
					expression_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(Not)).add(expression_AST));
					currentAST.root = expression_AST;
					currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
						expression_AST.getFirstChild() : expression_AST;
					currentAST.advanceChildToEnd();
				}
				expression_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==LITERAL_not) && (LA(2)==LPAREN)) {
				AST tmp7_AST = null;
				tmp7_AST = (AST)astFactory.create(LT(1));
				match(LITERAL_not);
				AST tmp8_AST = null;
				tmp8_AST = (AST)astFactory.create(LT(1));
				match(LPAREN);
				condition();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				AST tmp9_AST = null;
				tmp9_AST = (AST)astFactory.create(LT(1));
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					expression_AST = (AST)currentAST.root;
					expression_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(Not)).add(expression_AST));
					currentAST.root = expression_AST;
					currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
						expression_AST.getFirstChild() : expression_AST;
					currentAST.advanceChildToEnd();
				}
				expression_AST = (AST)currentAST.root;
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = expression_AST;
	}
	
	public final void expressionSearch() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expressionSearch_AST = null;
		
		if ((LA(1)==STRING)) {
			quotedRegularExpression(false);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			compareType();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			quotedRegularExpression(caseSensitive);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = (AST)currentAST.root;
				expressionSearch_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = (AST)currentAST.root;
		}
		else if ((LA(1)==FIELDTYPE) && (_tokenSet_2.member(LA(2))) && (LA(3)==STRING)) {
			simpleRegularExpression(false);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			compareType();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			quotedRegularExpression(caseSensitive);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = (AST)currentAST.root;
				expressionSearch_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = (AST)currentAST.root;
		}
		else if ((LA(1)==FIELDTYPE) && (_tokenSet_2.member(LA(2))) && (LA(3)==FIELDTYPE)) {
			simpleRegularExpression(false);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			compareType();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			simpleRegularExpression(caseSensitive);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = (AST)currentAST.root;
				expressionSearch_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = (AST)currentAST.root;
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		returnAST = expressionSearch_AST;
	}
	
	public final void compareType() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST compareType_AST = null;
		
		switch ( LA(1)) {
		case LITERAL_contains:
		{
			AST tmp10_AST = null;
			if (inputState.guessing==0) {
				tmp10_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp10_AST);
			}
			match(LITERAL_contains);
			compareType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_matches:
		{
			AST tmp11_AST = null;
			if (inputState.guessing==0) {
				tmp11_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp11_AST);
			}
			match(LITERAL_matches);
			compareType_AST = (AST)currentAST.root;
			break;
		}
		case EQUAL:
		{
			AST tmp12_AST = null;
			if (inputState.guessing==0) {
				tmp12_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp12_AST);
			}
			match(EQUAL);
			compareType_AST = (AST)currentAST.root;
			break;
		}
		case EEQUAL:
		{
			AST tmp13_AST = null;
			if (inputState.guessing==0) {
				tmp13_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp13_AST);
			}
			match(EEQUAL);
			compareType_AST = (AST)currentAST.root;
			break;
		}
		case NEQUAL:
		{
			AST tmp14_AST = null;
			if (inputState.guessing==0) {
				tmp14_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp14_AST);
			}
			match(NEQUAL);
			compareType_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = compareType_AST;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"and\"",
		"\"or\"",
		"\"not\"",
		"\"contains\"",
		"\"equals\"",
		"white space",
		"'('",
		"')'",
		"'='",
		"'=='",
		"'!='",
		"'\\\"'",
		"a text literal",
		"a letter",
		"a field type",
		"RegularExpression",
		"And",
		"Or",
		"Not",
		"ExpressionSearch",
		"LITERAL_matches"
	};
	
	private static final long _tokenSet_0_data_[] = { 328768L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long _tokenSet_1_data_[] = { 17134784L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long _tokenSet_2_data_[] = { 16806016L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	
	}
