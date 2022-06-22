package org.jabref.model.groups;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class AutomaticPersonsGroup extends AutomaticGroup {

    private final Field field;

    public AutomaticPersonsGroup(String name, GroupHierarchyType context, Field field) {
        super(name, context);
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutomaticPersonsGroup that = (AutomaticPersonsGroup) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticPersonsGroup(this.name.getValue(), this.context, this.field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        return LastNameGroup.getAsLastNamesLatexFree(field, entry)
                            .stream()
                            .map(lastName -> new LastNameGroup(lastName, GroupHierarchyType.INDEPENDENT, field, lastName))
                            .map(GroupTreeNode::new)
                            .collect(Collectors.toSet());
    }

    public Field getField() {
        return field;
    }
}
