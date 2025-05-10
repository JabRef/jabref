package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.field.StandardField;

/**
 * This group contains entries, which were automatically assigned to it.
 * Assignments are stored in the {@link StandardField#GROUPS} field.
 */
public class SmartGroup extends WordKeywordGroup {

    public SmartGroup(String name, GroupHierarchyType context, Character keywordSeparator) {
        super(name, context, StandardField.GROUPS, name, true, keywordSeparator, true);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new SmartGroup(getName(), getHierarchicalContext(), keywordSeparator);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExplicitGroup other)) {
            return false;
        }
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
                && Objects.equals(getIconName(), other.getIconName())
                && Objects.equals(getDescription(), other.getDescription())
                && Objects.equals(getColor(), other.getColor())
                && Objects.equals(isExpanded(), other.isExpanded());
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.getValue(), context, iconName, color, description, isExpanded);
    }
}
