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
    public ApplyResult apply() {
        if (!Objects.equals(node.getGroup(), before)) {
            return ApplyResult.of(this, "the group is '%s', not the recorded '%s'".formatted(node.getGroup().getName(), before.getName()));
        }
        node.setGroup(after);
        return ApplyResult.SUCCESS;
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
