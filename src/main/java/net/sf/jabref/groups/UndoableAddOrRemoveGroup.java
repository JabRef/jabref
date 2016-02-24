/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.l10n.Localization;

class UndoableAddOrRemoveGroup extends AbstractUndoableEdit {

    /** The root of the global groups tree */
    private final GroupTreeNode m_groupsRootHandle;
    /** The subtree that was added or removed */
    private final GroupTreeNode m_subtreeBackup;
    /**
     * In case of removing a node but keeping all of its children, the number of
     * children has to be stored.
     */
    private final int m_subtreeRootChildCount;
    /** The path to the edited subtree's root node */
    private final int[] m_pathToNode;
    /**
     * The type of the editing (ADD_NODE, REMOVE_NODE_KEEP_CHILDREN,
     * REMOVE_NODE_AND_CHILDREN)
     */
    private final int m_editType;
    private final GroupSelector m_groupSelector;
    private boolean mRevalidate = true;
    /** Adding of a single node (group). */
    public static final int ADD_NODE = 0;
    /** Removal of a single node. Children, if any, are kept. */
    public static final int REMOVE_NODE_KEEP_CHILDREN = 1;
    /** Removal of a node and all of its children. */
    public static final int REMOVE_NODE_AND_CHILDREN = 2;


    /**
     * Creates an object that can undo/redo an edit event.
     *
     * @param groupsRoot
     *            The global groups root.
     * @param editType
     *            The type of editing (ADD_NODE, REMOVE_NODE_KEEP_CHILDREN,
     *            REMOVE_NODE_AND_CHILDREN)
     * @param editedNode
     *            The edited node (which was added or will be removed). The node
     *            must be a descendant of node <b>groupsRoot</b>! This means
     *            that, in case of adding, you first have to add it to the tree,
     *            then call this constructor. When removing, you first have to
     *            call this constructor, then remove the node.
     */
    public UndoableAddOrRemoveGroup(GroupSelector gs, GroupTreeNode groupsRoot,
            GroupTreeNode editedNode, int editType) {
        m_groupSelector = gs;
        m_groupsRootHandle = groupsRoot;
        m_editType = editType;
        m_subtreeRootChildCount = editedNode.getChildCount();
        // storing a backup of the whole subtree is not required when children
        // are kept
        m_subtreeBackup = editType == UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN ? new GroupTreeNode(
                editedNode.getGroup().deepCopy()) : editedNode.deepCopy();
        // remember path to edited node. this cannot be stored as a reference,
        // because the reference itself might change. the method below is more
        // robust.
        m_pathToNode = editedNode.getIndexedPath();
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " + getName();
    }

    private String getName() {
        switch (m_editType) {
        case ADD_NODE:
            return Localization.lang("add group");
        case REMOVE_NODE_KEEP_CHILDREN:
            return Localization.lang("remove group (keep subgroups)");
        case REMOVE_NODE_AND_CHILDREN:
            return Localization.lang("remove group and subgroups");
        default:
            break;
        }
        return "? (" + Localization.lang("unknown edit") + ")";
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " + getName();
    }

    @Override
    public void undo() {
        super.undo();
        doOperation(true);
    }

    @Override
    public void redo() {
        super.redo();
        doOperation(false);
    }

    private void doOperation(boolean undo) {
        GroupTreeNode cursor = m_groupsRootHandle;
        final int childIndex = m_pathToNode[m_pathToNode.length - 1];
        // traverse path up to but last element
        for (int i = 0; i < (m_pathToNode.length - 1); ++i) {
            cursor = (GroupTreeNode) cursor.getChildAt(m_pathToNode[i]);
        }
        if (undo) {
            switch (m_editType) {
            case ADD_NODE:
                cursor.remove(childIndex);
                break;
            case REMOVE_NODE_KEEP_CHILDREN:
                // move all children to newNode, then add newNode
                GroupTreeNode newNode = m_subtreeBackup.deepCopy();
                for (int i = childIndex; i < (childIndex
                        + m_subtreeRootChildCount); ++i) {
                    newNode.add((GroupTreeNode) cursor.getChildAt(childIndex));
                }
                cursor.insert(newNode, childIndex);
                break;
            case REMOVE_NODE_AND_CHILDREN:
                cursor.insert(m_subtreeBackup.deepCopy(), childIndex);
                break;
            default:
                break;
            }
        } else { // redo
            switch (m_editType) {
            case ADD_NODE:
                cursor.insert(m_subtreeBackup.deepCopy(), childIndex);
                break;
            case REMOVE_NODE_KEEP_CHILDREN:
                // remove node, then insert all children
                GroupTreeNode removedNode = (GroupTreeNode) cursor
                        .getChildAt(childIndex);
                cursor.remove(childIndex);
                while (removedNode.getChildCount() > 0) {
                    cursor.insert((GroupTreeNode) removedNode.getFirstChild(),
                            childIndex);
                }
                break;
            case REMOVE_NODE_AND_CHILDREN:
                cursor.remove(childIndex);
                break;
            default:
                break;
            }
        }
        if (mRevalidate) {
            m_groupSelector.revalidateGroups();
        }
    }

    /**
     * Call this method to decide if the group list should be immediately
     * revalidated by this operation. Default is true.
     *
     * @param val
     *            a <code>boolean</code> value
     */
    public void setRevalidate(boolean val) {
        mRevalidate = val;
    }
}
