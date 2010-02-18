/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.search.SearchExpressionLexer;
import net.sf.jabref.search.SearchExpressionParser;
import net.sf.jabref.search.SearchExpressionTreeParser;
import net.sf.jabref.search.SearchExpressionTreeParserTokenTypes;
import net.sf.jabref.util.QuotedStringTokenizer;
import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * @author jzieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class SearchGroup extends AbstractGroup implements SearchRule {
	public static final String ID = "SearchGroup:";

	private final String m_searchExpression;

	private final boolean m_caseSensitive;

	private final boolean m_regExp;

	private final AST m_ast;

	private static final SearchExpressionTreeParser m_treeParser = new SearchExpressionTreeParser();

	/**
	 * If m_searchExpression is in valid syntax for advanced search, <b>this
	 * </b> will do the search; otherwise, either <b>RegExpRule </b> or
	 * <b>SimpleSearchRule </b> will be used.
	 */
	private final SearchRule m_searchRule;

	/**
	 * Creates a SearchGroup with the specified properties.
	 */
	public SearchGroup(String name, String searchExpression,
			boolean caseSensitive, boolean regExp, int context) {
		super(name, context);
		m_searchExpression = searchExpression;
		m_caseSensitive = caseSensitive;
		m_regExp = regExp;

		// create AST
		AST ast = null;
		try {
			SearchExpressionParser parser = new SearchExpressionParser(
					new SearchExpressionLexer(new StringReader(
							m_searchExpression)));
			parser.caseSensitive = m_caseSensitive;
			parser.regex = m_regExp;
			parser.searchExpression();
			ast = parser.getAST();
		} catch (Exception e) {
			ast = null;
			// nothing to do; set m_ast to null -> regular plaintext search
		}
		m_ast = ast;

		if (m_ast != null) { // do advanced search
			m_searchRule = this;
		} else { // do plaintext search
			if (m_regExp)
				m_searchRule = new RegExpRule(m_caseSensitive);
			else
				m_searchRule = new SimpleSearchRule(m_caseSensitive);
		}

	}

	/**
	 * Parses s and recreates the SearchGroup from it.
	 * 
	 * @param s
	 *            The String representation obtained from
	 *            SearchGroup.toString(), or null if incompatible
	 */
	public static AbstractGroup fromString(String s, BibtexDatabase db,
			int version) throws Exception {
		if (!s.startsWith(ID))
			throw new Exception(
					"Internal error: SearchGroup cannot be created from \"" + s
					+ "\". "
					+ "Please report this on www.sf.net/projects/jabref");
		QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
				.length()), SEPARATOR, QUOTE_CHAR);
		switch (version) {
		case 0:
		case 1:
		case 2: {
			String name = tok.nextToken();
			String expression = tok.nextToken();
			boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
			boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
			// version 0 contained 4 additional booleans to specify search
			// fields; these are ignored now, all fields are always searched
			return new SearchGroup(Util.unquote(name, QUOTE_CHAR), Util
					.unquote(expression, QUOTE_CHAR), caseSensitive, regExp,
					AbstractGroup.INDEPENDENT);
		}
		case 3: {
			String name = tok.nextToken();
			int context = Integer.parseInt(tok.nextToken());
			String expression = tok.nextToken();
			boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
			boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
			// version 0 contained 4 additional booleans to specify search
			// fields; these are ignored now, all fields are always searched
			return new SearchGroup(Util.unquote(name, QUOTE_CHAR), Util
					.unquote(expression, QUOTE_CHAR), caseSensitive, regExp,
					context);
		}
		default:
			throw new UnsupportedVersionException("SearchGroup", version);
		}
	}

    public String getTypeId() {
        return ID;
    }

    /**
	 * @see net.sf.jabref.groups.AbstractGroup#getSearchRule()
	 */
	public SearchRule getSearchRule() {
		return this;
	}

	/**
	 * Returns a String representation of this object that can be used to
	 * reconstruct it.
	 */
	public String toString() {
		return ID + Util.quote(m_name, SEPARATOR, QUOTE_CHAR) + SEPARATOR
				+ m_context + SEPARATOR
				+ Util.quote(m_searchExpression, SEPARATOR, QUOTE_CHAR)
				+ SEPARATOR + (m_caseSensitive ? "1" : "0") + SEPARATOR
				+ (m_regExp ? "1" : "0") + SEPARATOR;
	}

	public String getSearchExpression() {
		return m_searchExpression;
	}

	public boolean supportsAdd() {
		return false;
	}

	public boolean supportsRemove() {
		return false;
	}

	public AbstractUndoableEdit add(BibtexEntry[] entries) {
		// nothing to do, add is not supported
		return null;
	}

	public AbstractUndoableEdit remove(BibtexEntry[] entries) {
		// nothing to do, remove is not supported
		return null;
	}

	public boolean equals(Object o) {
		if (!(o instanceof SearchGroup))
			return false;
		SearchGroup other = (SearchGroup) o;
		return m_name.equals(other.m_name)
				&& m_searchExpression.equals(other.m_searchExpression)
				&& m_caseSensitive == other.m_caseSensitive
				&& m_regExp == other.m_regExp
                && getHierarchicalContext() == other.getHierarchicalContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jabref.groups.AbstractGroup#contains(java.util.Map,
	 *      net.sf.jabref.BibtexEntry)
	 */
	public boolean contains(Map<String, String> searchOptions, BibtexEntry entry) {
		return applyRule(searchOptions, entry) == 0 ? false : true;
	}

	public boolean contains(BibtexEntry entry) {
		// use dummy map
		return contains(new HashMap<String, String>(), entry);
	}

	public int applyRule(Map<String, String> searchOptions, BibtexEntry entry) {
		if (m_ast == null) {
			// the searchOptions object is a dummy; we need to insert
			// the actual search expression.
			searchOptions.put("option", m_searchExpression);
			return m_searchRule.applyRule(searchOptions, entry);
		}
		try {
			return m_treeParser.apply(m_ast, entry);
		} catch (RecognitionException e) {
			return 0; // this should never occur
		}
	}

	public AbstractGroup deepCopy() {
		try {
			return new SearchGroup(m_name, m_searchExpression, m_caseSensitive,
					m_regExp, m_context);
		} catch (Throwable t) {
			// this should never happen, because the constructor obviously
			// succeeded in creating _this_ instance!
			System.err.println("Internal error: Exception " + t
					+ " in SearchGroup.deepCopy(). "
					+ "Please report this on www.sf.net/projects/jabref");
			return null;
		}
	}

	public boolean isCaseSensitive() {
		return m_caseSensitive;
	}

	public boolean isRegExp() {
		return m_regExp;
	}

	public boolean isDynamic() {
		return true;
	}
	
	public String getDescription() {
		return getDescriptionForPreview(m_searchExpression, m_ast, m_caseSensitive,
				m_regExp);
	}
	
	public static String getDescriptionForPreview(String expr, AST ast,
			boolean caseSensitive, boolean regExp) {
		StringBuffer sb = new StringBuffer();
		if (ast == null) {
			sb.append(regExp ? Globals.lang(
			        "This group contains entries in which any field contains the regular expression <b>%0</b>",
                    Util.quoteForHTML(expr))
                    : Globals.lang(
                            "This group contains entries in which any field contains the term <b>%0</b>",
                            Util.quoteForHTML(expr)));
            sb.append(" (").append(caseSensitive ? Globals.lang("case sensitive")
                    : Globals.lang("case insensitive")).append("). ");
			sb.append(Globals.lang(
                    "Entries cannot be manually assigned to or removed from this group."));
            sb.append("<p><br>").append(Globals.lang(
                    "Hint%c To search specific fields only, enter for example%c<p><tt>author%esmith and title%eelectrical</tt>"));
			return sb.toString();
		}
		// describe advanced search expression
        sb.append(Globals.lang("This group contains entries in which")).append(" ");
		sb.append(describeNode(ast, regExp, false, false, false));
		sb.append(". ");
		sb.append(caseSensitive ? Globals.lang("The search is case sensitive.")
				: Globals.lang("The search is case insensitive."));
		return sb.toString();
	}

	protected static String describeNode(AST node, boolean regExp,
			boolean not, boolean and, boolean or) {
		StringBuffer sb = new StringBuffer();
		switch (node.getType()) {
		case SearchExpressionTreeParserTokenTypes.And:
			if (not)
                sb.append(Globals.lang("not")).append(" ");
			// if there was an "or" in this subtree so far, braces may be needed
			if (or || not)
				sb.append("(");
            sb.append(describeNode(node.getFirstChild(), regExp,
                    false, true, false)).append(" ").append(Globals.lang("and")).append(" ").append(describeNode(node.getFirstChild()
                    .getNextSibling(), regExp, false, true, false));
			if (or || not)
				sb.append(")");
			return sb.toString();
		case SearchExpressionTreeParserTokenTypes.Or:
			if (not)
                sb.append(Globals.lang("not")).append(" ");
			// if there was an "and" in this subtree so far, braces may be
			// needed
			if (and || not)
				sb.append("(");
            sb.append(describeNode(node.getFirstChild(), regExp,
                    false, false, true)).append(" ").append(Globals.lang("or")).append(" ").append(describeNode(node.getFirstChild()
                    .getNextSibling(), regExp, false, false, true));
			if (and || not)
				sb.append(")");
			return sb.toString();
		case SearchExpressionTreeParserTokenTypes.Not:
			return describeNode(node.getFirstChild(), regExp, !not,
					and, or);
		default:
			node = node.getFirstChild();
			final String field = node.getText();
			final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
			node = node.getNextSibling();
			final int type = node.getType();
			node = node.getNextSibling();
			final String termQuoted = Util.quoteForHTML(node.getText());
			final String fieldSpecQuoted = regExpFieldSpec ? Globals.lang(
					"any field that matches the regular expression <b>%0</b>",
                    Util.quoteForHTML(field)) : Globals.lang("the field <b>%0</b>", 
                            Util.quoteForHTML(field));
			switch (type) {
			case SearchExpressionTreeParserTokenTypes.LITERAL_contains:
			case SearchExpressionTreeParserTokenTypes.EQUAL:
				if (regExp)
					return not ? Globals.lang(
					        "%0 doesn't contain the Regular Expression <b>%1</b>",
                            fieldSpecQuoted, termQuoted)
							: Globals.lang(
							        "%0 contains the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
				return not ? Globals.lang(
						"%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                        termQuoted) : Globals.lang("%0 contains the term <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
			case SearchExpressionTreeParserTokenTypes.LITERAL_matches:
			case SearchExpressionTreeParserTokenTypes.EEQUAL:
				if (regExp)
					return not ? Globals.lang(
					        "%0 doesn't match the Regular Expression <b>%1</b>",
                            fieldSpecQuoted, termQuoted)
							: Globals.lang(
                                    "%0 matches the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
				return not ? Globals.lang(
						"%0 doesn't match the term <b>%1</b>", 
                        fieldSpecQuoted, termQuoted)
						: Globals.lang("%0 matches the term <b>%1</b>",
                                fieldSpecQuoted, 
                                termQuoted);
			case SearchExpressionTreeParserTokenTypes.NEQUAL:
				if (regExp)
					return not ? Globals.lang(
							"%0 contains the Regular Expression <b>%1</b>",
                            fieldSpecQuoted, termQuoted)
							: Globals.lang(
                                    "%0 doesn't contain the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
				return not ? Globals.lang("%0 contains the term <b>%1</b>",
                        fieldSpecQuoted, termQuoted) : Globals.lang(
						"%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                        termQuoted);
			default:
				return "Internal error: Unknown AST node type. "
						+ "Please report this on www.sf.net/projects/jabref";
				// this should never happen
			}
		}
	}

	public String getShortDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("<b>");
		if (Globals.prefs.getBoolean("groupShowDynamic"))
            sb.append("<i>").append(Util.quoteForHTML(getName())).append("</i>");
		else
			sb.append(Util.quoteForHTML(getName()));
            /*sb.append(Globals.lang("</b> - dynamic group (search expression: <b>")).*/
            sb.append(Globals.lang("</b> - dynamic group (")+ Globals.lang("search expression: <b>")).
                    
            append(Util.quoteForHTML(m_searchExpression)).append("</b>)");
		switch (getHierarchicalContext()) {
		case AbstractGroup.INCLUDING:
			sb.append(Globals.lang(", includes subgroups"));
			break;
		case AbstractGroup.REFINING:
			sb.append(Globals.lang(", refines supergroup"));
			break;
		default:
			break;
		}
		return sb.toString();
	}
}
