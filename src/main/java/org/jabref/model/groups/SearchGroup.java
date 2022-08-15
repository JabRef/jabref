package org.jabref.model.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.pdf.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.GroupSearchQuery;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
public class SearchGroup extends AbstractGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);
    private Set<BibEntry> matches = Set.of();
    private final GroupSearchQuery query;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        super(name, context);
        this.query = new GroupSearchQuery(searchExpression, searchFlags);
    }

    public String getSearchExpression() {
        return query.getSearchExpression();
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
        return query.getSearchFlags();
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

    public void updateMatches(BibDatabaseContext context) {
        try {
            this.matches = LuceneSearcher.of(context).search(query).keySet();
        } catch (IOException e) {
            LOGGER.warn("Could not open Index for: '{}'!\n{}", context.getDatabasePath().orElse(Path.of("unsaved")).toAbsolutePath(), e.getMessage());
        }
    }
}
