/**
 * RegExNode.java
 *
 * @author Created by Omnicore CodeGuide
 */

package net.sf.jabref.search;

import antlr.CommonAST;
import java.util.regex.Pattern;

public class RegExNode extends CommonAST {
	private Pattern pattern = null;
	public RegExNode(int tokenType, String text, boolean caseSensitive) {
		initialize(tokenType,text);
		pattern = Pattern.compile(text,caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
	}
	public Pattern getPattern() {
		return pattern;
	}
}

