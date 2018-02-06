package org.jabref.gui.groups;

import org.jabref.model.groups.GroupTreeNode;

public class MoveGroupChange {

    private GroupTreeNode oldParent;
    private int oldChildIndex;
    private GroupTreeNode newParent;
    private int newChildIndex;

    /**
     * @param oldParent
     * @param oldChildIndex
     * @param newParent The new parent node to which the node will be moved.
     * @param newChildIndex The child index at newParent to which the node will be moved.
     */
    public MoveGroupChange(GroupTreeNode oldParent, int oldChildIndex, GroupTreeNode newParent, int newChildIndex) {
        this.oldParent = oldParent;
        this.oldChildIndex = oldChildIndex;
        this.newParent = newParent;
        this.newChildIndex = newChildIndex;
    }

    public GroupTreeNode getOldParent() {
        return oldParent;
    }

    public int getOldChildIndex() {
        return oldChildIndex;
    }

    public GroupTreeNode getNewParent() {
        return newParent;
    }

    public int getNewChildIndex() {
        return newChildIndex;
    }

}
