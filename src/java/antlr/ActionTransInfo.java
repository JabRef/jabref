package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

/**
 * This class contains information about how an action
 * was translated (using the AST conversion rules).
 */
public class ActionTransInfo {
	public boolean assignToRoot = false;	// somebody did a "#rule = "
	public String refRuleRoot = null;		// somebody referenced #rule; string is translated var


	public String toString() {
		return "assignToRoot:"+assignToRoot+", refRuleRoot:"+refRuleRoot;
	}
}
