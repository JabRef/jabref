/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.groups;

import java.io.StringReader;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.search.*;
import net.sf.jabref.search.rules.RegExpSearchRule;
import net.sf.jabref.search.SearchRule;
import net.sf.jabref.search.rules.SimpleSearchRule;
import net.sf.jabref.util.QuotedStringTokenizer;
import antlr.RecognitionException;
import antlr.collections.AST;
import net.sf.jabref.util.StringUtil;

/**
 * @author jzieren
 *         <p>
 *         TODO To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Style - Code Templates
 */
public class SearchGroup extends AbstractGroup implements SearchRule {

    public static final String ID = "SearchGroup:";

    private final String searchExpression;

    private final boolean caseSensitive;

    private final boolean regExp;

    private final AST ast;

    private static final SearchExpressionTreeParser treeParser = new SearchExpressionTreeParser();

    /**
     * If searchExpression is in valid syntax for advanced search, <b>this
     * </b> will do the search; otherwise, either <b>RegExpSearchRule </b> or
     * <b>SimpleSearchRule </b> will be used.
     */
    private final SearchRule searchRule;


    /**
     * Creates a SearchGroup with the specified properties.
     */
    public SearchGroup(String name, String searchExpression,
                       boolean caseSensitive, boolean regExp, int context) {
        super(name, context);
        this.searchExpression = searchExpression;
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;

        // create AST
        AST ast;
        try {
            SearchExpressionParser parser = new SearchExpressionParser(
                    new SearchExpressionLexer(new StringReader(
                            this.searchExpression)));
            parser.caseSensitive = this.caseSensitive;
            parser.regex = this.regExp;
            parser.searchExpression();
            ast = parser.getAST();
        } catch (Exception e) {
            ast = null;
            // nothing to do; set ast to null -> regular plaintext search
        }
        this.ast = ast;

        if (this.ast != null) { // do advanced search
            searchRule = this;
        } else { // do plaintext search
            if (this.regExp) {
                searchRule = new RegExpSearchRule(this.caseSensitive);
            } else {
                searchRule = new SimpleSearchRule(this.caseSensitive);
            }
        }

    }

    /**
     * Parses s and recreates the SearchGroup from it.
     *
     * @param s The String representation obtained from
     *          SearchGroup.toString(), or null if incompatible
     */
    public static AbstractGroup fromString(String s, BibtexDatabase db,
                                           int version) throws Exception {
        if (!s.startsWith(SearchGroup.ID)) {
            throw new Exception(
                    "Internal error: SearchGroup cannot be created from \"" + s
                            + "\". "
                            + "Please report this on www.sf.net/projects/jabref");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(SearchGroup.ID
                .length()), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);
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
                return new SearchGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR), StringUtil
                        .unquote(expression, AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
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
                return new SearchGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR), StringUtil
                        .unquote(expression, AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
                        context);
            }
            default:
                throw new UnsupportedVersionException("SearchGroup", version);
        }
    }

    @Override
    public String getTypeId() {
        return SearchGroup.ID;
    }

    /**
     * @see net.sf.jabref.groups.AbstractGroup#getSearchRule()
     */
    @Override
    public SearchRule getSearchRule() {
        return this;
    }

    /**
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    @Override
    public String toString() {
        return SearchGroup.ID + StringUtil.quote(m_name, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) + AbstractGroup.SEPARATOR
                + m_context + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchExpression, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + (caseSensitive ? "1" : "0") + AbstractGroup.SEPARATOR
                + (regExp ? "1" : "0") + AbstractGroup.SEPARATOR;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    @Override
    public boolean supportsAdd() {
        return false;
    }

    @Override
    public boolean supportsRemove() {
        return false;
    }

    @Override
    public AbstractUndoableEdit add(BibtexEntry[] entries) {
        // nothing to do, add is not supported
        return null;
    }

    @Override
    public AbstractUndoableEdit remove(BibtexEntry[] entries) {
        // nothing to do, remove is not supported
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchGroup)) {
            return false;
        }
        SearchGroup other = (SearchGroup) o;
        return m_name.equals(other.m_name)
                && searchExpression.equals(other.searchExpression)
                && (caseSensitive == other.caseSensitive)
                && (regExp == other.regExp)
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.groups.AbstractGroup#contains(java.util.Map,
     *      net.sf.jabref.BibtexEntry)
     */
    @Override
    public boolean contains(String searchOptions, BibtexEntry entry) {
        return applyRule(searchOptions, entry) != 0;
    }

    @Override
    public boolean contains(BibtexEntry entry) {
        return contains("dummy", entry);
    }

    @Override
    public int applyRule(String searchOptions, BibtexEntry entry) {
        if (ast == null) {
            // the searchOptions object is a dummy; we need to insert
            // the actual search expression.
            return searchRule.applyRule(searchExpression, entry);
        }
        try {
            return SearchGroup.treeParser.apply(ast, entry);
        } catch (RecognitionException e) {
            return 0; // this should never occur
        }
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(m_name, searchExpression, caseSensitive,
                    regExp, m_context);
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
        return caseSensitive;
    }

    public boolean isRegExp() {
        return regExp;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getDescription() {
        return new SearchExpressionDescriber(caseSensitive,
                regExp, searchExpression, ast).getDescriptionForPreview();
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (Globals.prefs.getBoolean("groupShowDynamic")) {
            sb.append("<i>").append(StringUtil.quoteForHTML(getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(getName()));
        }
        sb.append("</b> - ");
        sb.append(Globals.lang("dynamic group"));
        sb.append(" (");
        sb.append(Globals.lang("search expression"));
        sb.append(" <b>").
                append(StringUtil.quoteForHTML(searchExpression)).append("</b>)");
        switch (getHierarchicalContext()) {
            case AbstractGroup.INCLUDING:
                sb.append(", ").append(Globals.lang("includes subgroups"));
                break;
            case AbstractGroup.REFINING:
                sb.append(", ").append(Globals.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }
}
