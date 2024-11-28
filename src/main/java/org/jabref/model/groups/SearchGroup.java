package org.jabref.model.groups;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import io.github.adr.linked.ADR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
public class SearchGroup extends AbstractGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);

    @ADR(38)
    private final Set<String> matchedEntries = new HashSet<>();

    private SearchQuery searchQuery;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        super(name, context);
        this.searchQuery = new SearchQuery(searchExpression, searchFlags);
    }

    /**
     * Used by {@link org.jabref.gui.importer.actions.SearchGroupsMigrationAction} to update the search expression.
     * <em>Do not use otherwise</em>.
     */
    public void setSearchExpression(String searchExpression) {
        LOGGER.debug("Setting search expression {}", searchExpression);
        this.searchQuery = new SearchQuery(searchExpression, searchQuery.getSearchFlags());
    }

    public String getSearchExpression() {
        return searchQuery.getSearchExpression();
    }

    public SearchQuery getSearchQuery() {
        return searchQuery;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchQuery.getSearchFlags();
    }

    public void setMatchedEntries(Collection<String> entriesId) {
        matchedEntries.clear();
        matchedEntries.addAll(entriesId);
    }

    public void updateMatches(BibEntry entry, boolean matched) {
        if (matched) {
            matchedEntries.add(entry.getId());
        } else {
            matchedEntries.remove(entry.getId());
        }
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
        return matchedEntries.contains(entry.getId());
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(getName(), getHierarchicalContext(), getSearchExpression(), getSearchFlags());
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            LOGGER.error("Internal error in SearchGroup.deepCopy(). " + "Please report this on https://github.com/JabRef/jabref/issues", t);
            return null;
        }
    }

    @Override
    public String toString() {
        return "SearchGroup [query=" + searchQuery + ", name=" + name + ", searchFlags=" + getSearchFlags() + ",  context=" + context + ", color=" + color + ", isExpanded=" + isExpanded + ", description=" + description + ", iconName=" + iconName + "]";
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext(), getSearchExpression(), getSearchFlags());
    }
}
