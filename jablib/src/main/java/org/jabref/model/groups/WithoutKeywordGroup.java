package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/// Matches entries that have no (non-blank) value in a given field.
///
/// This group is generated dynamically as a subgroup of an {@link AutomaticKeywordGroup} to collect
/// the entries that do not fall into any of the keyword subgroups (e.g. entries without a ranking).
/// It is never persisted on its own: the parent {@link AutomaticKeywordGroup} recreates it on the fly.
public class WithoutKeywordGroup extends AbstractGroup {

    private final Field field;

    public WithoutKeywordGroup(String name, GroupHierarchyType context, Field field) {
        super(name, context);
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    @Override
    public boolean contains(BibEntry entry) {
        return entry.getField(field)
                    .map(String::strip)
                    .map(String::isEmpty)
                    .orElse(true);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new WithoutKeywordGroup(getName(), context, field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WithoutKeywordGroup other)) {
            return false;
        }
        return Objects.equals(getName(), other.getName())
                && Objects.equals(context, other.context)
                && Objects.equals(field, other.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), context, field);
    }
}
