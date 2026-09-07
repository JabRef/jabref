package org.jabref.model.undo;

import java.util.Optional;

import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Replaces a library's group tree as a whole.
///
/// Whole tree rather than one node, because that is how every group operation already reaches the
/// model: the group panel edits its nodes in place and writes the root back with
/// [MetaData#setGroups]. One record therefore covers adding, removing, moving and reordering, and
/// undoing any of them is installing the tree that was there before.
///
/// Both states are copies of the *structure*: nodes are mutated in place, so a shared tree would
/// let a later edit rewrite what this change restores. The [org.jabref.model.groups.AbstractGroup]
/// each node carries is shared with the live tree, which is enough while operations replace a
/// node's group rather than mutate it.
///
/// The generated `equals` and `hashCode` walk both trees and the metadata, whose hash changes as
/// the library does — fine for comparing two changes, but do not put these in a hash-based
/// collection.
@NullMarked
public record UndoableGroupTreeChange(MetaData metaData, Optional<GroupTreeNode> before, Optional<GroupTreeNode> after) implements BibChange {

    public UndoableGroupTreeChange {
        before = before.map(GroupTreeNode::copySubtree);
        after = after.map(GroupTreeNode::copySubtree);
    }

    @Override
    public UndoableGroupTreeChange inverted() {
        return new UndoableGroupTreeChange(metaData, after, before);
    }

    @Override
    public ApplyResult apply() {
        after.map(GroupTreeNode::copySubtree)
             .ifPresentOrElse(metaData::setGroups, metaData::clearGroups);
        return ApplyResult.SUCCESS;
    }
}
