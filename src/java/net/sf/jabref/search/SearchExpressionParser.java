// $ANTLR : "Parser.g" -> "SearchExpressionParser.java"$

package net.sf.jabref.search;
import java.io.StringReader;

import antlr.*;
import antlr.collections.AST;
import antlr.collections.impl.ASTArray;
import antlr.collections.impl.BitSet;

public class SearchExpressionParser extends antlr.LLkParser       implements SearchExpressionParserTokenTypes
 {

	public boolean caseSensitive = false;
    public boolean regex = true;
	/** Creates a parser and lexer instance and tests the specified String.
	  * Returns the AST if s is in valid syntax for advanced field search, null otherwise. */
	public static AST checkSyntax(String s, boolean caseSensitive, boolean regex) {
		// Is there some way to prevent instance creation here?
		// How can a parser and/or lexer be reused?
		SearchExpressionParser parser = new SearchExpressionParser(new SearchExpressionLexer(
				new StringReader(s)));
		parser.caseSensitive = caseSensitive;
		parser.regex = regex;
		try {
			parser.searchExpression();
			return parser.getAST();
		} catch (Exception e) {
			return null;
		}
	}

protected SearchExpressionParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public SearchExpressionParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected SearchExpressionParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public SearchExpressionParser(TokenStream lexer) {
  this(lexer,3);
}

public SearchExpressionParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final void quotedRegularExpression(
		boolean caseSensitive, boolean regex
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST quotedRegularExpression_AST = null;
		Token  var_s = null;
		AST var_s_AST = null;
		
		var_s = LT(1);
		var_s_AST = astFactory.create(var_s);
		astFactory.addASTChild(currentAST, var_s_AST);
		match(STRING);
		if ( inputState.guessing==0 ) {
			quotedRegularExpression_AST = currentAST.root;
			
							quotedRegularExpression_AST = astFactory.make((new ASTArray(2)).add(new RegExNode(RegularExpression,var_s.getText(),caseSensitive,regex)).add(quotedRegularExpression_AST));
						
			currentAST.root = quotedRegularExpression_AST;
			currentAST.child = quotedRegularExpression_AST!=null &&quotedRegularExpression_AST.getFirstChild()!=null ?
				quotedRegularExpression_AST.getFirstChild() : quotedRegularExpression_AST;
			currentAST.advanceChildToEnd();
		}
		quotedRegularExpression_AST = currentAST.root;
		returnAST = quotedRegularExpression_AST;
	}
	
	public final void simpleRegularExpression(
		boolean caseSensitive, boolean regex
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST simpleRegularExpression_AST = null;
		Token  var_s = null;
		AST var_s_AST = null;
		
		var_s = LT(1);
		var_s_AST = astFactory.create(var_s);
		astFactory.addASTChild(currentAST, var_s_AST);
		match(FIELDTYPE);
		if ( inputState.guessing==0 ) {
			simpleRegularExpression_AST = currentAST.root;
			
							simpleRegularExpression_AST = astFactory.make((new ASTArray(2)).add(new RegExNode(RegularExpression,var_s.getText(),caseSensitive,regex)).add(simpleRegularExpression_AST));
						
			currentAST.root = simpleRegularExpression_AST;
			currentAST.child = simpleRegularExpression_AST!=null &&simpleRegularExpression_AST.getFirstChild()!=null ?
				simpleRegularExpression_AST.getFirstChild() : simpleRegularExpression_AST;
			currentAST.advanceChildToEnd();
		}
		simpleRegularExpression_AST = currentAST.root;
		returnAST = simpleRegularExpression_AST;
	}
	
	public final void searchExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST searchExpression_AST = null;
		
		condition();
		astFactory.addASTChild(currentAST, returnAST);
		AST tmp1_AST = null;
		tmp1_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp1_AST);
		match(Token.EOF_TYPE);
		searchExpression_AST = currentAST.root;
		returnAST = searchExpression_AST;
	}
	
	public final void condition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST condition_AST = null;
		
		boolean synPredMatched80 = false;
		if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
			int _m80 = mark();
			synPredMatched80 = true;
			inputState.guessing++;
			try {
				{
				expression();
				match(LITERAL_and);
				condition();
				}
			}
			catch (RecognitionException pe) {
				synPredMatched80 = false;
			}
			rewind(_m80);
inputState.guessing--;
		}
		if ( synPredMatched80 ) {
			expression();
			astFactory.addASTChild(currentAST, returnAST);
			match(LITERAL_and);
			condition();
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				condition_AST = currentAST.root;
				condition_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(And)).add(condition_AST));
				currentAST.root = condition_AST;
				currentAST.child = condition_AST!=null &&condition_AST.getFirstChild()!=null ?
					condition_AST.getFirstChild() : condition_AST;
				currentAST.advanceChildToEnd();
			}
			condition_AST = currentAST.root;
		}
		else {
			boolean synPredMatched82 = false;
			if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
				int _m82 = mark();
				synPredMatched82 = true;
				inputState.guessing++;
				try {
					{
					expression();
					match(LITERAL_or);
					condition();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched82 = false;
				}
				rewind(_m82);
inputState.guessing--;
			}
			if ( synPredMatched82 ) {
				expression();
				astFactory.addASTChild(currentAST, returnAST);
				match(LITERAL_or);
				condition();
				astFactory.addASTChild(currentAST, returnAST);
				if ( inputState.guessing==0 ) {
					condition_AST = currentAST.root;
					condition_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(Or)).add(condition_AST));
					currentAST.root = condition_AST;
					currentAST.child = condition_AST!=null &&condition_AST.getFirstChild()!=null ?
						condition_AST.getFirstChild() : condition_AST;
					currentAST.advanceChildToEnd();
				}
				condition_AST = currentAST.root;
			}
			else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3)))) {
				expression();
				astFactory.addASTChild(currentAST, returnAST);
				condition_AST = currentAST.root;
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
			astFactory.addASTChild(currentAST, returnAST);
			expression_AST = currentAST.root;
			break;
		}
		case LPAREN:
		{
			match(LPAREN);
			condition();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			expression_AST = currentAST.root;
			break;
		}
		default:
			if ((LA(1)==LITERAL_not) && (LA(2)==STRING||LA(2)==FIELDTYPE)) {
				match(LITERAL_not);
				expressionSearch();
				astFactory.addASTChild(currentAST, returnAST);
				if ( inputState.guessing==0 ) {
					expression_AST = currentAST.root;
					expression_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(Not)).add(expression_AST));
					currentAST.root = expression_AST;
					currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
						expression_AST.getFirstChild() : expression_AST;
					currentAST.advanceChildToEnd();
				}
				expression_AST = currentAST.root;
			}
			else if ((LA(1)==LITERAL_not) && (LA(2)==LPAREN)) {
				match(LITERAL_not);
				match(LPAREN);
				condition();
				astFactory.addASTChild(currentAST, returnAST);
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					expression_AST = currentAST.root;
					expression_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(Not)).add(expression_AST));
					currentAST.root = expression_AST;
					currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
						expression_AST.getFirstChild() : expression_AST;
					currentAST.advanceChildToEnd();
				}
				expression_AST = currentAST.root;
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
		
		if ((LA(1)==STRING) && (_tokenSet_2.member(LA(2))) && (LA(3)==STRING)) {
			quotedRegularExpression(false,true);
			astFactory.addASTChild(currentAST, returnAST);
			compareType();
			astFactory.addASTChild(currentAST, returnAST);
			quotedRegularExpression(caseSensitive,regex);
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = currentAST.root;
				expressionSearch_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = currentAST.root;
		}
		else if ((LA(1)==FIELDTYPE) && (_tokenSet_2.member(LA(2))) && (LA(3)==STRING)) {
			simpleRegularExpression(false,true);
			astFactory.addASTChild(currentAST, returnAST);
			compareType();
			astFactory.addASTChild(currentAST, returnAST);
			quotedRegularExpression(caseSensitive,regex);
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = currentAST.root;
				expressionSearch_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = currentAST.root;
		}
		else if ((LA(1)==FIELDTYPE) && (_tokenSet_2.member(LA(2))) && (LA(3)==FIELDTYPE)) {
			simpleRegularExpression(false,true);
			astFactory.addASTChild(currentAST, returnAST);
			compareType();
			astFactory.addASTChild(currentAST, returnAST);
			simpleRegularExpression(caseSensitive,regex);
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = currentAST.root;
				expressionSearch_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = currentAST.root;
		}
		else if ((LA(1)==STRING) && (_tokenSet_2.member(LA(2))) && (LA(3)==FIELDTYPE)) {
			quotedRegularExpression(false,true);
			astFactory.addASTChild(currentAST, returnAST);
			compareType();
			astFactory.addASTChild(currentAST, returnAST);
			simpleRegularExpression(caseSensitive,regex);
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				expressionSearch_AST = currentAST.root;
				expressionSearch_AST = astFactory.make( (new ASTArray(2)).add(astFactory.create(ExpressionSearch)).add(expressionSearch_AST));
				currentAST.root = expressionSearch_AST;
				currentAST.child = expressionSearch_AST!=null &&expressionSearch_AST.getFirstChild()!=null ?
					expressionSearch_AST.getFirstChild() : expressionSearch_AST;
				currentAST.advanceChildToEnd();
			}
			expressionSearch_AST = currentAST.root;
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
			tmp10_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp10_AST);
			match(LITERAL_contains);
			compareType_AST = currentAST.root;
			break;
		}
		case LITERAL_matches:
		{
			AST tmp11_AST = null;
			tmp11_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp11_AST);
			match(LITERAL_matches);
			compareType_AST = currentAST.root;
			break;
		}
		case EQUAL:
		{
			AST tmp12_AST = null;
			tmp12_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp12_AST);
			match(EQUAL);
			compareType_AST = currentAST.root;
			break;
		}
		case EEQUAL:
		{
			AST tmp13_AST = null;
			tmp13_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp13_AST);
			match(EEQUAL);
			compareType_AST = currentAST.root;
			break;
		}
		case NEQUAL:
		{
			AST tmp14_AST = null;
			tmp14_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp14_AST);
			match(NEQUAL);
			compareType_AST = currentAST.root;
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
		"\"matches\"",
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
		"ExpressionSearch"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	}

     private static final long[] mk_tokenSet_0() {
         long[] data = { 328768L, 0L};
         return data;
     }
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 357824L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 29056L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	
	}
