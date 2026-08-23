package org.jabref.model.undo;

import java.util.List;
import java.util.Objects;

import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

/// Wholesale replacement of a group node's children.
///
/// `before` and `after` are detached snapshots, not live nodes: applying copies their children
/// onto the target rather than moving them, so the same change can be applied repeatedly. A
/// consequence, inherited from the edit this replaces, is that undo restores *equal* nodes
/// rather than the same objects, so anything holding a `GroupTreeNode` reference across an
/// undo is left pointing at a detached node.
///
/// The target is addressed by its index path from `root` and resolved on each apply, because
/// the node object at that position is itself replaced by the operations this records.
///
/// Only the children are replaced. The target's own group is left alone, matching the previous
/// behaviour — `GroupChange` also reassigns the root group, and undoing that was never
/// supported.
@NullMarked
public record UndoableModifySubtree(GroupTreeNode root, List<Integer> path, GroupTreeNode before, GroupTreeNode after) implements BibChange {

    public UndoableModifySubtree {
        path = List.copyOf(path);
    }

    @Override
    public UndoableModifySubtree inverted() {
        return new UndoableModifySubtree(root, path, after, before);
    }

    @Override
    public void apply() {
        GroupTreeNode target = root.getDescendant(path).orElse(null);
        if (target == null) {
            return;
        }
        target.removeAllChildren();
        for (GroupTreeNode child : after.getChildren()) {
            child.copySubtree().moveTo(target);
        }
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableModifySubtree other)
                && ChangeIdentity.same(root, other.root)
                && path.equals(other.path)
                && ChangeIdentity.same(before, other.before)
                && ChangeIdentity.same(after, other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(root), path, ChangeIdentity.hash(before), ChangeIdentity.hash(after));
    }
}
