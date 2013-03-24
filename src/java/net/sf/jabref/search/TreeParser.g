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

    private static final int PSEUDOFIELD_TYPE = 1;

    public int apply(AST ast, BibtexEntry bibtexEntry) throws antlr.RecognitionException {
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
			int pseudoField = 0;
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
						content = (String)bibtexEntry.getField(searchKeys[i].toString());
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
		}
	)
	;

