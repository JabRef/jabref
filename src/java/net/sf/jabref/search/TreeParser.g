header {
package net.sf.jabref.search;
import java.util.*;
import java.util.regex.*;
import net.sf.jabref.*;
}

class SearchExpressionTreeParser extends TreeParser;

options {
	importVocab = SearchExpressionParser;
	exportVocab = SearchExpressionTreeParser;
}

{
	private static final int MATCH_EXACT = 0;
	private static final int MATCH_CONTAINS = 1;
	private static final int MATCH_DOES_NOT_CONTAIN = 2;

	private BibtexEntry bibtexEntry;
	private Object[] searchKeys;
	// JabRefPreferences
	private boolean caseSensitiveSearch = false;

    public int apply(JabRefPreferences prefs, AST ast, BibtexEntry bibtexEntry) throws antlr.RecognitionException {
		this.caseSensitiveSearch = prefs.getBoolean("caseSensitiveSearch");
		this.bibtexEntry = bibtexEntry;
		// specification of fields to search is done in the search expression itself
		this.searchKeys = bibtexEntry.getAllFields();
		return tSearchExpression(ast) ? 1 : 0;
	}
}



// ---------- Condition and Expressions ----------

tSearchExpression returns [boolean ret = false;] throws PatternSyntaxException
{
	boolean a = false, b = false;
}
	: // predicates for and/or used to evaluate 2nd expression only if necessary
	#( And a=tSearchExpression ( {a}? b=tSearchExpression | . ) ) { ret = a && b; }
	|
	#( Or a=tSearchExpression ( {!a}? b=tSearchExpression | . ) ) { ret = a || b; }
	|
	#( Not a=tSearchExpression ) { ret = !a; }
	|
	ret=tExpressionSearch
	;

tSearchType returns [ int matchType = 0; ]
	:
	LITERAL_contains { matchType = MATCH_CONTAINS; } 
	| 
	LITERAL_matches { matchType = MATCH_EXACT; } 
	| 
	EQUAL { matchType = MATCH_CONTAINS; }
	| 
	EEQUAL { matchType = MATCH_EXACT; }
	| 
	NEQUAL { matchType = MATCH_DOES_NOT_CONTAIN; }
	;

tExpressionSearch returns [ boolean ret = false; ] throws PatternSyntaxException
{
	int matchType = 0;
}
	:
	#( ExpressionSearch var_f:RegularExpression matchType=tSearchType var_v:RegularExpression 
		{
			Pattern fieldSpec = ((RegExNode)var_f).getPattern();
			Pattern valueSpec = ((RegExNode)var_v).getPattern();
			boolean pseudoField_type;
			for (int i = 0; i <= searchKeys.length && !ret; ++i) {
				pseudoField_type = i == searchKeys.length; // additional (pseudo) field
				String field = !pseudoField_type ? searchKeys[i].toString() : "type"; // pseudo field
				if (!fieldSpec.matcher(field).matches()) 
					continue;
				Matcher matcher = valueSpec.matcher(!pseudoField_type ? bibtexEntry.getField(field).toString()
					: bibtexEntry.getType().getName().toLowerCase());
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
		}
	)
	;

