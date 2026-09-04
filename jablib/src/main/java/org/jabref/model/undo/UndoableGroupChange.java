package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

/// Replaces a group definition while retaining its position in the group tree.
@NullMarked
public record UndoableGroupChange(GroupTreeNode node, AbstractGroup before, AbstractGroup after) implements BibChange {

    @Override
    public UndoableGroupChange inverted() {
        return new UndoableGroupChange(node, after, before);
    }

    @Override
    public void apply() {
        node.setGroup(after);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableGroupChange other)
                && ChangeIdentity.same(node, other.node)
                && Objects.equals(before, other.before)
                && Objects.equals(after, other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(node), before, after);
    }
}
