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
package net.sf.jabref.gui.groups;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.groups.*;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class GroupTreeNodeViewModel implements Transferable, TreeNode {

    private static final int MAX_DISPLAYED_LETTERS = 35;
    private static final Icon GROUP_REFINING_ICON = IconTheme.JabRefIcon.GROUP_REFINING.getSmallIcon();
    private static final Icon GROUP_INCLUDING_ICON = IconTheme.JabRefIcon.GROUP_INCLUDING.getSmallIcon();
    private static final Icon GROUP_REGULAR_ICON = null;

    public static final DataFlavor FLAVOR;
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.logic.groups.GroupTreeNode");
        } catch (ClassNotFoundException e) {
            // never happens
        }
        FLAVOR = df;
        FLAVORS = new DataFlavor[] {GroupTreeNodeViewModel.FLAVOR};
    }

    private final GroupTreeNode node;

    public GroupTreeNodeViewModel(GroupTreeNode node) {
        this.node = node;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return GroupTreeNodeViewModel.FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(GroupTreeNodeViewModel.FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        return this;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return new GroupTreeNodeViewModel(node.getChildAt(childIndex));
    }

    @Override
    public int getChildCount() {
        return node.getChildCount();
    }

    @Override
    public TreeNode getParent() {
        return new GroupTreeNodeViewModel(node.getParent());
    }

    @Override
    public int getIndex(TreeNode child) {
        if(! (child instanceof GroupTreeNodeViewModel)) {
            return -1;
        }

        GroupTreeNodeViewModel childViewModel = (GroupTreeNodeViewModel)child;
        return node.getIndex(childViewModel.getNode());
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return node.isLeaf();
    }

    @Override
    public Enumeration children() {
        Iterable<GroupTreeNode> children = node.children();
        return new Enumeration() {

            @Override
            public boolean hasMoreElements() {
                return children.iterator().hasNext();
            }

            @Override
            public Object nextElement() {
                return children.iterator().next();
            }
        };
    }

    public GroupTreeNode getNode() {
        return node;
    }

    /** Collapse this node and all its children. */
    public void collapseSubtree(JTree tree) {
        tree.collapsePath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.collapseSubtree(tree);
        }
    }

    /** Expand this node and all its children. */
    public void expandSubtree(JTree tree) {
        tree.expandPath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.collapseSubtree(tree);
        }
    }

    public List<GroupTreeNodeViewModel> getChildren() {
        List<GroupTreeNodeViewModel> children = new ArrayList<>();
        for(GroupTreeNode child : node.children()) {
            children.add(new GroupTreeNodeViewModel(child));
        }
        return children;
    }

    boolean printInItalics() {
        return Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC) &&  node.getGroup().isDynamic();
    }

    public String getText() {
        AbstractGroup group = node.getGroup();
        String name = StringUtil.limitStringLength(group.getName(), MAX_DISPLAYED_LETTERS);
        StringBuilder sb = new StringBuilder(60);
        sb.append(name);

        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS)) {
            if (group instanceof ExplicitGroup) {
                sb.append(" [").append(((ExplicitGroup) group).getNumEntries()).append(']');
            } else if ((group instanceof KeywordGroup) || (group instanceof SearchGroup)) {
                int hits = 0;
                BasePanel currentBasePanel = JabRef.jrf.getCurrentBasePanel();
                if(currentBasePanel != null) {
                    for (BibEntry entry : currentBasePanel.getDatabase().getEntries()) {
                        if (group.contains(entry)) {
                            hits++;
                        }
                    }
                }
                sb.append(" [").append(hits).append(']');
            }
        }

        return sb.toString();
    }

    public String getDescription() {
        return "<html>" + node.getGroup().getShortDescription() + "</html>";
    }

    public Icon getIcon() {
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_ICONS)) {
            switch (node.getGroup().getHierarchicalContext()) {
            case REFINING:
                return GROUP_REFINING_ICON;
            case INCLUDING:
                return GROUP_INCLUDING_ICON;
            default:
                return GROUP_REGULAR_ICON;
            }
        } else {
            return null;
        }
    }

    public TreePath getTreePath() {
        List<GroupTreeNode> pathToNode = node.getPath();
        return new TreePath(pathToNode.stream().map(node -> new GroupTreeNodeViewModel(node)).toArray());
    }
}
