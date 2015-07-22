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
package net.sf.jabref.groups.structure;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.search.describer.BasicSearchDescriber;
import net.sf.jabref.search.describer.SearchExpressionDescriber;
import net.sf.jabref.search.rules.RegExpSearchRule;
import net.sf.jabref.search.SearchRule;
import net.sf.jabref.search.rules.SearchExpression;
import net.sf.jabref.search.rules.SimpleSearchRule;
import net.sf.jabref.util.QuotedStringTokenizer;
import net.sf.jabref.util.StringUtil;

/**
 * Internally, it consists of a search pattern.
 *
 * @author jzieren
 */
public class SearchGroup extends AbstractGroup {

    public static final String ID = "SearchGroup:";

    private final String searchExpression;
    private final boolean caseSensitive;
    private final boolean regExp;

    /**
     * If searchExpression is in valid syntax for advanced search, <b>this
     * </b> will do the search; otherwise, either <b>RegExpSearchRule </b> or
     * <b>SimpleSearchRule </b> will be used.
     */
    private final SearchRule searchRule;
    private final SearchExpression expressionSearchRule;


    /**
     * Creates a SearchGroup with the specified properties.
     */
    public SearchGroup(String name, String searchExpression, boolean caseSensitive, boolean regExp, GroupHierarchyType context) {
        super(name, context);
        this.searchExpression = searchExpression;
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;

        expressionSearchRule = new SearchExpression(caseSensitive, regExp);
        if (expressionSearchRule.validateSearchStrings(this.searchExpression)) {
            searchRule = expressionSearchRule;  // do advanced search
        } else if (this.regExp) {
            searchRule = new RegExpSearchRule(this.caseSensitive);
        } else {
            searchRule = new SimpleSearchRule(this.caseSensitive);
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
                        GroupHierarchyType.INDEPENDENT);
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
                        GroupHierarchyType.getByNumber(context));
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
     * @see AbstractGroup#getSearchRule()
     */
    @Override
    public SearchRule getSearchRule() {
        return this.searchRule;
    }

    /**
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    @Override
    public String toString() {
        return SearchGroup.ID + StringUtil.quote(name, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) + AbstractGroup.SEPARATOR
                + context + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchExpression, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(caseSensitive) + AbstractGroup.SEPARATOR
                + StringUtil.booleanToBinaryString(regExp) + AbstractGroup.SEPARATOR;
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
        return name.equals(other.name)
                && searchExpression.equals(other.searchExpression)
                && (caseSensitive == other.caseSensitive)
                && (regExp == other.regExp)
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.groups.structure.AbstractGroup#contains(java.util.Map,
     *      net.sf.jabref.BibtexEntry)
     */
    @Override
    public boolean contains(String searchOptions, BibtexEntry entry) {
        return getSearchRule().applyRule(searchOptions, entry) != 0;
    }

    @Override
    public boolean contains(BibtexEntry entry) {
        return contains(SearchRule.DUMMY_QUERY, entry);
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(name, searchExpression, caseSensitive,
                    regExp, context);
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
        if(expressionSearchRule.getTree() != null) {
            return new SearchExpressionDescriber(caseSensitive, regExp, expressionSearchRule.getTree()).getDescription();
        } else {
            return new BasicSearchDescriber(caseSensitive, regExp, searchExpression).getDescription();
        }
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC)) {
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
            case INCLUDING:
                sb.append(", ").append(Globals.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Globals.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

}
