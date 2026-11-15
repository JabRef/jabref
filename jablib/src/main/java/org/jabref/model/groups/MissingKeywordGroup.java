package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/// Matches entries that have no keyword in a given field.
///
/// [AutomaticKeywordGroup] creates it as a subgroup next to the keyword subgroups,
/// so that entries without a value (e.g., without a ranking) can still be found.
/// The field is split the same way as in [AutomaticKeywordGroup], so a field consisting only of delimiters counts as empty.
@NullMarked
public class MissingKeywordGroup extends KeywordGroup {

    /// Not localized, because group names are serialized. The user interface localizes the displayed name.
    private static final String NAME = "No value";

    private final Character keywordDelimiter;

    public MissingKeywordGroup(GroupHierarchyType context, Field searchField, Character keywordDelimiter) {
        super(NAME, context, searchField, "", true);
        this.keywordDelimiter = keywordDelimiter;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }

    @Override
    public boolean contains(BibEntry entry) {
        return entry.getFieldAsKeywords(searchField, keywordDelimiter)
                    .stream()
                    .allMatch(keyword -> keyword.get().isBlank());
    }

    @Override
    public AbstractGroup deepCopy() {
        return new MissingKeywordGroup(getHierarchicalContext(), searchField, keywordDelimiter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MissingKeywordGroup other)) {
            return false;
        }
        return Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
                && Objects.equals(searchField, other.searchField)
                && Objects.equals(keywordDelimiter, other.keywordDelimiter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHierarchicalContext(), searchField, keywordDelimiter);
    }
}
