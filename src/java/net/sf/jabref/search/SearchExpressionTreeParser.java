// $ANTLR : "TreeParser.g" -> "SearchExpressionTreeParser.java"$

package net.sf.jabref.search;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.layout.format.RemoveLatexCommands;
import antlr.MismatchedTokenException;
import antlr.NoViableAltException;
import antlr.RecognitionException;
import antlr.collections.AST;

@SuppressWarnings({"unused"})
public class SearchExpressionTreeParser extends antlr.TreeParser       implements SearchExpressionTreeParserTokenTypes
 {

    // Formatter used on every field before searching. Removes Latex commands that
    // may interfere with the search:
    static RemoveLatexCommands removeLatexCommands = new RemoveLatexCommands();

    private static final int MATCH_EXACT = 0;
	private static final int MATCH_CONTAINS = 1;
	private static final int MATCH_DOES_NOT_CONTAIN = 2;

	private BibtexEntry bibtexEntry;
	private Object[] searchKeys;

    private static final int PSEUDOFIELD_TYPE = 1;

     public int apply(AST ast, BibtexEntry bibtexEntry) throws antlr.RecognitionException {
		this.bibtexEntry = bibtexEntry;
		// specification of fields to search is done in the search expression itself
		this.searchKeys = bibtexEntry.getAllFields().toArray();
		return tSearchExpression(ast) ? 1 : 0;
	}
public SearchExpressionTreeParser() {
	tokenNames = _tokenNames;
}

	public final boolean  tSearchExpression(AST _t) throws RecognitionException, PatternSyntaxException {
		boolean ret = false;

        AST tSearchExpression_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
			boolean a = false, b = false;
		
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case And:
			{
				AST __t87 = _t;
				AST tmp1_AST_in = _t;
				match(_t,And);
				_t = _t.getFirstChild();
				a=tSearchExpression(_t);
				_t = _retTree;
				{
				if (_t==null) _t=ASTNULL;
				if ((((_t.getType() >= And && _t.getType() <= ExpressionSearch)))&&(a)) {
					b=tSearchExpression(_t);
					_t = _retTree;
				}
				else if (((_t.getType() >= LITERAL_and && _t.getType() <= ExpressionSearch))) {
					AST tmp2_AST_in = _t;
					if ( _t==null ) throw new MismatchedTokenException();
					_t = _t.getNextSibling();
				}
				else {
					throw new NoViableAltException(_t);
				}
				
				}
				_t = __t87;
				_t = _t.getNextSibling();
				ret = a && b;
				break;
			}
			case Or:
			{
				AST __t89 = _t;
				AST tmp3_AST_in = _t;
				match(_t,Or);
				_t = _t.getFirstChild();
				a=tSearchExpression(_t);
				_t = _retTree;
				{
				if (_t==null) _t=ASTNULL;
				if ((((_t.getType() >= And && _t.getType() <= ExpressionSearch)))&&(!a)) {
					b=tSearchExpression(_t);
					_t = _retTree;
				}
				else if (((_t.getType() >= LITERAL_and && _t.getType() <= ExpressionSearch))) {
					AST tmp4_AST_in = _t;
					if ( _t==null ) throw new MismatchedTokenException();
					_t = _t.getNextSibling();
				}
				else {
					throw new NoViableAltException(_t);
				}
				
				}
				_t = __t89;
				_t = _t.getNextSibling();
				ret = a || b;
				break;
			}
			case Not:
			{
				AST __t91 = _t;
				AST tmp5_AST_in = _t;
				match(_t,Not);
				_t = _t.getFirstChild();
				a=tSearchExpression(_t);
				_t = _retTree;
				_t = __t91;
				_t = _t.getNextSibling();
				ret = !a;
				break;
			}
			case ExpressionSearch:
			{
				ret=tExpressionSearch(_t);
				_t = _retTree;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return ret;
	}
	
	public final boolean  tExpressionSearch(AST _t) throws RecognitionException, PatternSyntaxException {
		 boolean ret = false;

		AST var_f = null;
		AST var_v = null;
		
			int matchType = 0;
		
		
		try {      // for error handling
			AST __t94 = _t;
			AST tmp6_AST_in = _t;
			match(_t,ExpressionSearch);
			_t = _t.getFirstChild();
			var_f = _t;
			match(_t,RegularExpression);
			_t = _t.getNextSibling();
			matchType=tSearchType(_t);
			_t = _retTree;
			var_v = _t;
			match(_t,RegularExpression);
			_t = _t.getNextSibling();
			
						Pattern fieldSpec = ((RegExNode)var_f).getPattern();
						Pattern valueSpec = ((RegExNode)var_v).getPattern();
			boolean noSuchField = true;
						// this loop iterates over all regular keys, then over pseudo keys like "type"
						for (int i = 0; i < searchKeys.length + PSEUDOFIELD_TYPE && !ret; ++i) {
							String content;
							switch (i - searchKeys.length + 1) {
								case PSEUDOFIELD_TYPE:
									if (!fieldSpec.matcher("entrytype").matches())
										continue;
									content = bibtexEntry.getType().getName();
									break;
								default: // regular field
									if (!fieldSpec.matcher(searchKeys[i].toString()).matches())
										continue;
									content = removeLatexCommands.format(
                                            bibtexEntry.getField(searchKeys[i].toString()));
							}
			noSuchField = false;
							if (content == null)
								continue; // paranoia
							Matcher matcher = valueSpec.matcher(content);
							switch (matchType) {
							case MATCH_CONTAINS:
								ret = matcher.find();
								break;
							case MATCH_EXACT:
								ret = matcher.matches();
								break;
							case MATCH_DOES_NOT_CONTAIN:
								ret = !matcher.find();
								break;
							}
						}
			if (noSuchField && matchType == MATCH_DOES_NOT_CONTAIN)
			ret = true; // special case
					
			_t = __t94;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return ret;
	}
	
	public final int  tSearchType(AST _t) throws RecognitionException {
		 int matchType = 0;

        AST tSearchType_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_contains:
			{
				AST tmp7_AST_in = _t;
				match(_t,LITERAL_contains);
				_t = _t.getNextSibling();
				matchType = MATCH_CONTAINS;
				break;
			}
			case LITERAL_matches:
			{
				AST tmp8_AST_in = _t;
				match(_t,LITERAL_matches);
				_t = _t.getNextSibling();
				matchType = MATCH_EXACT;
				break;
			}
			case EQUAL:
			{
				AST tmp9_AST_in = _t;
				match(_t,EQUAL);
				_t = _t.getNextSibling();
				matchType = MATCH_CONTAINS;
				break;
			}
			case EEQUAL:
			{
				AST tmp10_AST_in = _t;
				match(_t,EEQUAL);
				_t = _t.getNextSibling();
				matchType = MATCH_EXACT;
				break;
			}
			case NEQUAL:
			{
				AST tmp11_AST_in = _t;
				match(_t,NEQUAL);
				_t = _t.getNextSibling();
				matchType = MATCH_DOES_NOT_CONTAIN;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return matchType;
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
	
	}
	
