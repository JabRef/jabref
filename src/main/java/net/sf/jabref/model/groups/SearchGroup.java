package net.sf.jabref.model.groups;

import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.GroupSearchQuery;
import net.sf.jabref.model.search.rules.SearchRule;
import net.sf.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internally, it consists of a search pattern.
 *
 * @author jzieren
 */
public class SearchGroup extends AbstractGroup {

    public static final String ID = "SearchGroup:";

    private final GroupSearchQuery query;

    private static final Log LOGGER = LogFactory.getLog(SearchGroup.class);

    private final String searchExpression;
    private final boolean caseSensitive;
    private final boolean regExp;


    /**
     * Creates a SearchGroup with the specified properties.
     */
    public SearchGroup(String name, String searchExpression, boolean caseSensitive, boolean regExp,
            GroupHierarchyType context) {
        super(name, context);

        this.searchExpression = searchExpression;
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.query = new GroupSearchQuery(searchExpression, caseSensitive, regExp);
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
        return SearchGroup.ID + StringUtil.quote(getName(), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + getContext().ordinal() + AbstractGroup.SEPARATOR
                + StringUtil.quote(getSearchExpression(), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(isCaseSensitive())
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(isRegExp()) + AbstractGroup.SEPARATOR;
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
    public Optional<EntriesGroupChange> add(List<BibEntry> entriesToAdd) {
        throw new UnsupportedOperationException("Search group does not support adding entries.");
    }

    @Override
    public Optional<EntriesGroupChange> remove(List<BibEntry> entriesToRemove) {
        throw new UnsupportedOperationException("Search group does not support removing entries.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchGroup)) {
            return false;
        }
        SearchGroup other = (SearchGroup) o;
        return getName().equals(other.getName())
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
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    public SearchRule getSearchRule() {
        return query.getRule();
    }

}
