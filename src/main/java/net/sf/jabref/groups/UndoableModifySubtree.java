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

import java.util.List;
import java.util.Vector;

import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.l10n.Localization;

public class UndoableModifySubtree extends AbstractUndoableEdit {

    /** A backup of the groups before the modification */
    private final GroupTreeNode m_groupRoot;
    private final GroupTreeNode m_subtreeBackup;
    /** The path to the global groups root node */
    private final int[] m_subtreeRootPath;
    private final GroupSelector m_groupSelector;
    /** This holds the new subtree (the root's modified children) to allow redo. */
    private final List<TreeNode> m_modifiedSubtree = new Vector<>();
    private boolean mRevalidate = true;
    private final String m_name;


    /**
     *
     * @param subtree
     *            The root node of the subtree that was modified (this node may
     *            not be modified, it is just used as a convenience handle).
     */
    public UndoableModifySubtree(GroupSelector groupSelector, GroupTreeNode groupRoot,
            GroupTreeNode subtree, String name) {
        m_subtreeBackup = subtree.deepCopy();
        m_groupRoot = groupRoot;
        m_subtreeRootPath = subtree.getIndexedPath();
        m_groupSelector = groupSelector;
        m_name = name;
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " + m_name;

    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " + m_name;
    }

    @Override
    public void undo() {
        super.undo();
        // remember modified children for redo
        m_modifiedSubtree.clear();
        // get node to edit
        final GroupTreeNode subtreeRoot = m_groupRoot
                .getNode(m_subtreeRootPath);
        for (int i = 0; i < subtreeRoot.getChildCount(); ++i) {
            m_modifiedSubtree.add(subtreeRoot.getChildAt(i));
        }
        // keep subtree handle, but restore everything else from backup
        subtreeRoot.removeAllChildren();
        for (int i = 0; i < m_subtreeBackup.getChildCount(); ++i) {
            subtreeRoot.add(((GroupTreeNode) m_subtreeBackup.getChildAt(i))
                    .deepCopy());
        }
        if (mRevalidate) {
            m_groupSelector.revalidateGroups();
        }
    }

    @Override
    public void redo() {
        super.redo();
        final GroupTreeNode subtreeRoot = m_groupRoot
                .getNode(m_subtreeRootPath);
        subtreeRoot.removeAllChildren();
        for (int i = 0; i < m_modifiedSubtree.size(); ++i) {
            subtreeRoot.add((GroupTreeNode) m_modifiedSubtree.get(i));
        }
        if (mRevalidate) {
            m_groupSelector.revalidateGroups();
        }
    }

    /**
     * Call this method to decide if the group list should be immediately
     * revalidated by this operation. Default is true.
     */
    public void setRevalidate(boolean revalidate) {
        mRevalidate = revalidate;
    }
}
