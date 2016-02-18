/*  Copyright (C) 2003-2016 JabRef contributors.
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

import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.QuotedStringTokenizer;
import net.sf.jabref.logic.util.strings.StringUtil;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internally, it consists of a search pattern.
 *
 * @author jzieren
 */
public class SearchGroup extends AbstractGroup {

    public static final String ID = "SearchGroup:";

    private final SearchQuery query;

    private static final Log LOGGER = LogFactory.getLog(SearchGroup.class);


    /**
     * Creates a SearchGroup with the specified properties.
     */
    public SearchGroup(String name, String searchExpression, boolean caseSensitive, boolean regExp, GroupHierarchyType context) {
        super(name, context);

        this.query = new SearchQuery(searchExpression, caseSensitive, regExp);
    }

    /**
     * Parses s and recreates the SearchGroup from it.
     *
     * @param s The String representation obtained from
     *          SearchGroup.toString(), or null if incompatible
     */
    public static AbstractGroup fromString(String s, BibDatabase db,
            int version) throws Exception {
        if (!s.startsWith(SearchGroup.ID)) {
            throw new Exception(
                    "Internal error: SearchGroup cannot be created from \"" + s
                            + "\". "
                    + "Please report this on https://github.com/JabRef/jabref/issues");
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
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    @Override
    public String toString() {
        return SearchGroup.ID + StringUtil.quote(name, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + context.ordinal() + AbstractGroup.SEPARATOR
                + StringUtil.quote(getSearchExpression(), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(isCaseSensitive())
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(isRegExp()) + AbstractGroup.SEPARATOR;
    }

    public String getSearchExpression() {
        return this.query.getQuery();
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
    public AbstractUndoableEdit add(List<BibEntry> entries) {
        // nothing to do, add is not supported
        return null;
    }

    @Override
    public AbstractUndoableEdit remove(List<BibEntry> entries) {
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
                && this.getSearchExpression().equals(other.getSearchExpression())
                && (this.isCaseSensitive() == other.isCaseSensitive())
                && (isRegExp() == other.isRegExp())
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return this.query.isMatch(entry);
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(getName(), getSearchExpression(), isCaseSensitive(),
                    isRegExp(), getHierarchicalContext());
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            LOGGER.error("Internal error in SearchGroup.deepCopy(). "
                    + "Please report this on https://github.com/JabRef/jabref/issues", t);
            return null;
        }
    }

    public boolean isCaseSensitive() {
        return this.query.isCaseSensitive();
    }

    public boolean isRegExp() {
        return this.query.isRegularExpression();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getDescription() {
        return this.query.getDescription();
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
        sb.append(Localization.lang("dynamic group"));
        sb.append(" (");
        sb.append(Localization.lang("search expression"));
        sb.append(" <b>").
                append(StringUtil.quoteForHTML(getSearchExpression())).append("</b>)");
        switch (getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

}
