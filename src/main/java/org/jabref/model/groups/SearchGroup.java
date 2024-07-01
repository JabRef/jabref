package org.jabref.model.groups;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.search.LuceneManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.GroupSearchQuery;
import org.jabref.model.search.SearchFlags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
public class SearchGroup extends AbstractGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);
    private final String searchExpression;
    private final EnumSet<SearchFlags> searchFlags;
    private Set<BibEntry> matches = Set.of();
    private GroupSearchQuery query;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        super(name, context);
        this.searchExpression = searchExpression;
        this.searchFlags = searchFlags;
    }

    public String getSearchExpression() {
        return query.getSearchExpression();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchGroup other)) {
            return false;
        }
        return Objects.equals(getName(), other.getName())
               && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
               && Objects.equals(getSearchExpression(), other.getSearchExpression())
               && Objects.equals(getSearchFlags(), other.getSearchFlags());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return matches.contains(entry);
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(getName(), getHierarchicalContext(), getSearchExpression(), getSearchFlags());
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            LOGGER.error("Internal error in SearchGroup.deepCopy(). "
                    + "Please report this on https://github.com/JabRef/jabref/issues", t);
            return null;
        }
    }

    @Override
    public String toString() {
        return "SearchGroup [query=" + query + ", name=" + name + ", searchFlags=" + getSearchFlags() + ",  context=" + context + ", color=" + color + ", isExpanded=" + isExpanded + ", description=" + description + ", iconName=" + iconName + "]";
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext(), getSearchExpression(), getSearchFlags());
    }

    public void updateMatches(LuceneManager luceneManager) {
        if (query == null) {
            query = new GroupSearchQuery(searchExpression, searchFlags);
        }
        this.matches = luceneManager.search(query).getAllSearchResults().keySet();
    }
}
