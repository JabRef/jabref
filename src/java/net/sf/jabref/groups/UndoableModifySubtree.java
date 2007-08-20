/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.groups;

import java.util.Vector;

import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.Globals;

public class UndoableModifySubtree extends AbstractUndoableEdit {
    /** A backup of the groups before the modification */
    private final GroupTreeNode m_subtreeBackup;
    /** The path to the global groups root node */
    private final int[] m_subtreeRootPath;
    private final GroupSelector m_groupSelector;
    /** This holds the new subtree (the root's modified children) to allow redo. */
    private Vector<TreeNode> m_modifiedSubtree = new Vector<TreeNode>();
    private boolean m_revalidate = true;
    private final String m_name;

    /**
     * 
     * @param subtree
     *            The root node of the subtree that was modified (this node may
     *            not be modified, it is just used as a convenience handle).
     */
    public UndoableModifySubtree(GroupSelector groupSelector,
            GroupTreeNode subtree, String name) {
        m_subtreeBackup = subtree.deepCopy();
        m_subtreeRootPath = subtree.getIndexedPath();
        m_groupSelector = groupSelector;
        m_name = name;
    }

    public String getUndoPresentationName() {
        return Globals.lang("Undo") + ": " + m_name;
        // JZTODO lyrics
    }

    public String getRedoPresentationName() {
        return Globals.lang("Redo") + ": " + m_name;
    }

    public void undo() {
        super.undo();
        // remember modified children for redo
        m_modifiedSubtree.clear();
        // get node to edit
        final GroupTreeNode subtreeRoot = m_groupSelector.getGroupTreeRoot()
                .getNode(m_subtreeRootPath);
        for (int i = 0; i < subtreeRoot.getChildCount(); ++i)
            m_modifiedSubtree.add(subtreeRoot.getChildAt(i));
        // keep subtree handle, but restore everything else from backup
        subtreeRoot.removeAllChildren();
        for (int i = 0; i < m_subtreeBackup.getChildCount(); ++i)
            subtreeRoot.add(((GroupTreeNode) m_subtreeBackup.getChildAt(i))
                    .deepCopy());
        if (m_revalidate)
            m_groupSelector.revalidateGroups();
    }

    public void redo() {
        super.redo();
        final GroupTreeNode subtreeRoot = m_groupSelector.getGroupTreeRoot()
                .getNode(m_subtreeRootPath);
        subtreeRoot.removeAllChildren();
        for (int i = 0; i < m_modifiedSubtree.size(); ++i)
            subtreeRoot.add((GroupTreeNode) m_modifiedSubtree.elementAt(i));
        if (m_revalidate)
            m_groupSelector.revalidateGroups();
    }

    /**
     * Call this method to decide if the group list should be immediately
     * revalidated by this operation. Default is true.
     */
    public void setRevalidate(boolean revalidate) {
        m_revalidate = revalidate;
    }
}
