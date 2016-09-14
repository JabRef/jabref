package net.sf.jabref.gui.groups;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.gui.undo.AbstractUndoableJabRefEdit;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.MoveGroupChange;

/**
 * @author jzieren
 *
 */
class UndoableMoveGroup extends AbstractUndoableJabRefEdit {

    private final GroupTreeNodeViewModel root;
    private final List<Integer> pathToNewParent;
    private final int newChildIndex;
    private final List<Integer> pathToOldParent;
    private final int oldChildIndex;


    public UndoableMoveGroup(GroupTreeNodeViewModel root, MoveGroupChange moveChange) {
        this.root = Objects.requireNonNull(root);
        Objects.requireNonNull(moveChange);
        pathToOldParent = moveChange.getOldParent().getIndexedPathFromRoot();
        pathToNewParent = moveChange.getNewParent().getIndexedPathFromRoot();
        oldChildIndex = moveChange.getOldChildIndex();
        newChildIndex = moveChange.getNewChildIndex();
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("move group");
    }

    @Override
    public void undo() {
        super.undo();

        GroupTreeNode newParent = root.getNode().getDescendant(pathToNewParent).get(); //TODO: NULL
        GroupTreeNode node = newParent.getChildAt(newChildIndex).get(); //TODO: Null
        //TODO: NULL
        node.moveTo(root.getNode().getDescendant(pathToOldParent).get(), oldChildIndex);
    }

    @Override
    public void redo() {
        super.redo();

        GroupTreeNode oldParent = root.getNode().getDescendant(pathToOldParent).get(); //TODO: NULL
        GroupTreeNode node = oldParent.getChildAt(oldChildIndex).get(); //TODO:Null
        //TODO: NULL
        node.moveTo(root.getNode().getDescendant(pathToNewParent).get(), newChildIndex);
    }
}
