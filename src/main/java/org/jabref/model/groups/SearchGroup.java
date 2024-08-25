package org.jabref.model.groups;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
public class SearchGroup extends AbstractGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);
    private final ObservableMap<Integer, BibEntry> matchedEntries = FXCollections.observableHashMap();
    private final SearchQuery query;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        super(name, context);
        this.query = new SearchQuery(searchExpression, searchFlags);
    }

    public String getSearchExpression() {
        return query.getSearchExpression();
    }

    public SearchQuery getQuery() {
        return query;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return query.getSearchFlags();
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
        return matchedEntries.containsKey(System.identityHashCode(entry));
    }

    public void setMatchedEntries(Collection<BibEntry> entries) {
        matchedEntries.clear();
        entries.forEach(entry -> matchedEntries.put(System.identityHashCode(entry), entry));
    }

    public void updateEntry(BibEntry entry, boolean matched) {
        if (matched) {
            matchedEntries.put(System.identityHashCode(entry), entry);
        } else {
            matchedEntries.remove(System.identityHashCode(entry));
        }
    }

    public ObservableMap<Integer, BibEntry> getMatchedEntries() {
        return matchedEntries;
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
}
