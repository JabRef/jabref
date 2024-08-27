package org.jabref.model.groups;

import java.util.EnumSet;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.search.LuceneManager;
import org.jabref.logic.util.Version;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
@AllowedToUseLogic("because it needs access to lucene manager")
public class SearchGroup extends AbstractGroup {

    // We cannot have this constant in Version java because of recursion errors
    // Thus, we keep it here, because it is (currently) used only in the context of groups.
    public static final Version VERSION_6_0_ALPHA = Version.parse("6.0-alpha");
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);

    private final ObservableMap<Integer, BibEntry> matchedEntries = FXCollections.observableHashMap();
    private SearchQuery query;
    private LuceneManager luceneManager;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags, LuceneManager luceneManager) {
        super(name, context);
        this.query = new SearchQuery(searchExpression, searchFlags);
        this.luceneManager = luceneManager;
        updateMatches();
    }

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        this(name, context, searchExpression, searchFlags, null);
    }

    public String getSearchExpression() {
        return query.getSearchExpression();
    }

    /**
     * Used by {@link org.jabref.gui.importer.actions.SearchGroupsMigrationAction} to update the search expression.
     * <em>Do not use otherwise</em>.
     */
    public void setSearchExpression(String searchExpression) {
        LOGGER.debug("Setting search expression {}", searchExpression);
        this.query = new SearchQuery(searchExpression, query.getSearchFlags());
    }

    public SearchQuery getQuery() {
        return query;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return query.getSearchFlags();
    }

    public void setLuceneManager(LuceneManager luceneManager) {
        this.luceneManager = luceneManager;
    }

    public void updateMatches() {
        if (luceneManager == null) {
            return;
        }
        matchedEntries.clear();
        // TODO: Search should be done in a background thread
        luceneManager.search(query).getMatchedEntries().forEach(entry -> matchedEntries.put(System.identityHashCode(entry), entry));
    }

    public void updateMatches(BibEntry entry) {
        if (luceneManager == null) {
            return;
        }
        if (luceneManager.isMatched(entry, query)) {
            matchedEntries.put(System.identityHashCode(entry), entry);
        } else {
            matchedEntries.remove(System.identityHashCode(entry));
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
        return matchedEntries.containsKey(System.identityHashCode(entry));
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(getName(), getHierarchicalContext(), getSearchExpression(), getSearchFlags(), luceneManager);
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
