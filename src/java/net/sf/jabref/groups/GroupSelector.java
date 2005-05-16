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

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.undo.*;

import net.sf.jabref.*;

public class GroupSelector extends SidePaneComponent implements
        TreeSelectionListener, ActionListener, ErrorMessageDisplay {
    JButton newButton = new JButton(new ImageIcon(GUIGlobals.newSmallIconFile)),
            helpButton = new JButton(
                    new ImageIcon(GUIGlobals.helpSmallIconFile)),
            refresh = new JButton(
                    new ImageIcon(GUIGlobals.refreshSmallIconFile)),
            autoGroup = new JButton(new ImageIcon(GUIGlobals.autoGroupIcon)),
            openset = new JButton(Globals.lang("Settings"));
    Color bgColor = Color.white;
    GroupsTree groupsTree;
    DefaultTreeModel groupsTreeModel;
    GroupTreeNode groupsRoot;
    JScrollPane sp;
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JabRefFrame frame;

    String searchField;
    JPopupMenu groupsContextMenu = new JPopupMenu();
    JPopupMenu settings = new JPopupMenu();
    private JRadioButtonMenuItem hideNonHits, grayOut;
    JRadioButtonMenuItem groupModeUnion = new JRadioButtonMenuItem(Globals
            .lang("Include subgroups"), true);
    JRadioButtonMenuItem groupModeIntersection = new JRadioButtonMenuItem(
            Globals.lang("Intersection with supergroups"), false);
    JRadioButtonMenuItem groupModeIndependent = new JRadioButtonMenuItem(
            Globals.lang("Independent"), false);
    JRadioButtonMenuItem andCb = new JRadioButtonMenuItem(Globals
            .lang("Intersection"), true);
    JRadioButtonMenuItem orCb = new JRadioButtonMenuItem(Globals.lang("Union"),
            false);
    JRadioButtonMenuItem floatCb = new JRadioButtonMenuItem(Globals
            .lang("Float"), true);
    JRadioButtonMenuItem highlCb = new JRadioButtonMenuItem(Globals
            .lang("Highlight"), false);
    JCheckBoxMenuItem invCb = new JCheckBoxMenuItem(Globals.lang("Inverted"),
            false), select = new JCheckBoxMenuItem(Globals
            .lang("Select matches"), false);
    // JMenu
    // moreRow = new JMenuItem(Globals.lang("Size of groups interface (rows)"));
    // lessRow = new JMenuItem(Globals.lang("Show one less rows"));
    ButtonGroup groupModeGroup = new ButtonGroup();
    ButtonGroup bgr = new ButtonGroup();
    ButtonGroup visMode = new ButtonGroup();
    ButtonGroup nonHits = new ButtonGroup();
    JButton expand = new JButton(new ImageIcon(GUIGlobals.downIconFile)),
            reduce = new JButton(new ImageIcon(GUIGlobals.upIconFile));
    SidePaneManager manager;


    /**
     * The first element for each group defines which field to use for the
     * quicksearch. The next two define the name and regexp for the group.
     * 
     * @param groupData
     *            The group meta data in raw format.
     */
    public GroupSelector(JabRefFrame frame, SidePaneManager manager) {
        super(manager, GUIGlobals.groupsIconFile, Globals.lang("Groups"));
        this.groupsRoot = new GroupTreeNode(new AllEntriesGroup());

        this.manager = manager;
        this.frame = frame;
        hideNonHits = new JRadioButtonMenuItem(Globals.lang("Hide non-hits"),
                !Globals.prefs.getBoolean("grayOutNonHits"));
        grayOut = new JRadioButtonMenuItem(Globals.lang("Gray out non-hits"),
                Globals.prefs.getBoolean("grayOutNonHits"));
        nonHits.add(hideNonHits);
        nonHits.add(grayOut);
        floatCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupFloatSelections", floatCb.isSelected());
            }
        });
        andCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupIntersectSelections", andCb
                        .isSelected());
            }
        });
        invCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupInvertSelections", invCb.isSelected());
            }
        });
        select.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupSelectMatches", select.isSelected());
            }
        });
        grayOut.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("grayOutNonHits", grayOut.isSelected());
            }
        });
        groupModeIndependent.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupSubgroupIndependent", groupModeIndependent.isSelected());
            }
        });

        groupModeIntersection.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupSubgroupIntersection", groupModeIntersection.isSelected());
            }
        });
        
        if (Globals.prefs.getBoolean("groupFloatSelections")) {

            floatCb.setSelected(true);
            highlCb.setSelected(false);
        } else {
            highlCb.setSelected(true);
            floatCb.setSelected(false);
        }
        if (Globals.prefs.getBoolean("groupIntersectSelections")) {
            andCb.setSelected(true);
            orCb.setSelected(false);
        } else {
            orCb.setSelected(true);
            andCb.setSelected(false);
        }

        //defaults.put("groupSubgroupIndependent", Boolean.FALSE);
        //defaults.put("groupSubgroupIntersection", Boolean.TRUE);
        if (Globals.prefs.getBoolean("groupSubgroupIndependent")) {
            groupModeIndependent.setSelected(true);
            groupModeIntersection.setSelected(false);
            groupModeUnion.setSelected(false);
        } else {
            groupModeIndependent.setSelected(false);
            if (Globals.prefs.getBoolean("groupSubgroupIntersection")) {
                groupModeIntersection.setSelected(true);
                groupModeUnion.setSelected(false);
            } else {
                groupModeIntersection.setSelected(false);
                groupModeUnion.setSelected(true);
            }
                
        }
        
        invCb.setSelected(Globals.prefs.getBoolean("groupInvertSelections"));
        select.setSelected(Globals.prefs.getBoolean("groupSelectMatches"));

        openset.setMargin(new Insets(0, 0, 0, 0));
        settings.add(groupModeUnion);
        settings.add(groupModeIntersection);
        settings.add(groupModeIndependent);
        settings.addSeparator();
        settings.add(andCb);
        settings.add(orCb);
        settings.addSeparator();
        settings.add(invCb);
        settings.addSeparator();
        settings.add(highlCb);
        settings.add(floatCb);
        settings.addSeparator();
        settings.add(select);
        settings.addSeparator();
        settings.add(grayOut);
        settings.add(hideNonHits);

        // settings.add(moreRow);
        // settings.add(lessRow);
        openset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (settings.isVisible()) {
                    // System.out.println("oee");
                    // settings.setVisible(false);
                } else {
                    JButton src = (JButton) e.getSource();
                    settings.show(src, 0, openset.getHeight());
                }
            }
        });
        expand.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = Globals.prefs.getInt("groupsVisibleRows") + 1;
                groupsTree.setVisibleRowCount(i);
                groupsTree.revalidate();
                groupsTree.repaint();
                GroupSelector.this.revalidate();
                GroupSelector.this.repaint();
                Globals.prefs.putInt("groupsVisibleRows", i);
            }
        });
        reduce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = Globals.prefs.getInt("groupsVisibleRows") - 1;
                if (i < 1)
                    i = 1;
                groupsTree.setVisibleRowCount(i);
                groupsTree.revalidate();
                groupsTree.repaint();
                GroupSelector.this.revalidate();
                // _panel.sidePaneManager.revalidate();
                GroupSelector.this.repaint();
                Globals.prefs.putInt("groupsVisibleRows", i);
            }
        });
        Dimension butDim = new Dimension(20, 20);
        Dimension butDim2 = new Dimension(40, 20);
        newButton.setPreferredSize(butDim);
        newButton.setMinimumSize(butDim);
        refresh.setPreferredSize(butDim);
        refresh.setMinimumSize(butDim);
        helpButton.setPreferredSize(butDim);
        helpButton.setMinimumSize(butDim);
        autoGroup.setPreferredSize(butDim);
        autoGroup.setMinimumSize(butDim);
        openset.setPreferredSize(butDim2);
        openset.setMinimumSize(butDim2);
        expand.setPreferredSize(butDim);
        expand.setMinimumSize(butDim);
        reduce.setPreferredSize(butDim);
        reduce.setMinimumSize(butDim);
        Insets butIns = new Insets(0, 0, 0, 0);
        helpButton.setMargin(butIns);
        reduce.setMargin(butIns);
        expand.setMargin(butIns);
        openset.setMargin(butIns);
        newButton.addActionListener(this);
        refresh.addActionListener(this);
        andCb.addActionListener(this);
        orCb.addActionListener(this);
        invCb.addActionListener(this);
        autoGroup.addActionListener(this);
        newButton.setToolTipText(Globals.lang("New group"));
        refresh.setToolTipText(Globals.lang("Refresh view"));
        andCb.setToolTipText(Globals
                .lang("Display only entries belonging to all selected"
                        + " groups."));
        orCb.setToolTipText(Globals
                .lang("Display all entries belonging to one or more "
                        + "of the selected groups."));
        autoGroup.setToolTipText(Globals
                .lang("Automatically create groups for database."));
        invCb.setToolTipText(Globals
                .lang("Show entries *not* in group selection"));
        floatCb.setToolTipText(Globals
                .lang("Move entries in group selection to the top"));
        highlCb.setToolTipText(Globals
                .lang("Gray out entries not in group selection"));
        select
                .setToolTipText(Globals
                        .lang("Select entries in group selection"));
        expand.setToolTipText(Globals.lang("Show one more row"));
        reduce.setToolTipText(Globals.lang("Show one less rows"));
        groupModeGroup.add(groupModeUnion);
        groupModeGroup.add(groupModeIntersection);
        groupModeGroup.add(groupModeIndependent);
        bgr.add(andCb);
        bgr.add(orCb);
        visMode.add(floatCb);
        visMode.add(highlCb);
        JPanel main = new JPanel();
        main.setLayout(gbl);
        /*SidePaneHeader header = new SidePaneHeader("Groups",
                GUIGlobals.groupsIconFile, this);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(header, con);
        main.add(header);*/
        con.fill = GridBagConstraints.BOTH;
        //con.insets = new Insets(0, 0, 2, 0);
        con.weightx = 1;
        con.gridwidth = 1;
        //con.insets = new Insets(1, 1, 1, 1);
        gbl.setConstraints(newButton, con);
        main.add(newButton);
        gbl.setConstraints(refresh, con);
        main.add(refresh);
        gbl.setConstraints(autoGroup, con);
        main.add(autoGroup);
        con.gridwidth = GridBagConstraints.REMAINDER;
        HelpAction helpAction = new HelpAction(frame.helpDiag,
                GUIGlobals.groupsHelp, "Help on groups");
        helpButton.addActionListener(helpAction);
        helpButton.setToolTipText(Globals.lang("Help on groups"));
        gbl.setConstraints(helpButton, con);
        main.add(helpButton);
        // header.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        // helpButton.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        groupsTree = new GroupsTree(this);
        groupsTree.addTreeSelectionListener(this);
        groupsTree.setModel(groupsTreeModel = new DefaultTreeModel(groupsRoot));
        sp = new JScrollPane(groupsTree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        revalidateGroups();
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weighty = 1;
        gbl.setConstraints(sp, con);
        main.add(sp);
        JPanel pan = new JPanel();
        GridBagLayout gb = new GridBagLayout();
        con.weighty = 0;
        gbl.setConstraints(pan, con);
        pan.setLayout(gb);
        con.weightx = 1;
        con.gridwidth = 1;
        gb.setConstraints(openset, con);
        pan.add(openset);
        con.weightx = 0;
        gb.setConstraints(expand, con);
        pan.add(expand);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gb.setConstraints(reduce, con);
        pan.add(reduce);
        main.add(pan);
        main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        add(main, BorderLayout.CENTER);
        definePopup();
    }

    private void definePopup() {
        // These key bindings are just to have the shortcuts displayed
        // in the popup menu. The actual keystroke processing is in 
        // BasePanel (entryTable.addKeyListener(...)).
        moveNodeUpAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
        moveNodeDownAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
        moveNodeLeftAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
        moveNodeRightAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));
        groupsContextMenu.add(addGroupAction);
        groupsContextMenu.add(addSubgroupAction);
        groupsContextMenu.add(editGroupAction);
        groupsContextMenu.add(removeGroupAndSubgroupsAction);
        groupsContextMenu.add(removeGroupKeepSubgroupsAction);
        groupsContextMenu.add(moveSubmenu);
        groupsContextMenu.add(expandSubtreeAction);
        groupsContextMenu.add(collapseSubtreeAction);
        groupsContextMenu.add(showOverlappingGroupsAction);
        groupsContextMenu.add(clearHighlightAction);
        moveSubmenu.add(moveNodeUpAction);
        moveSubmenu.add(moveNodeDownAction);
        moveSubmenu.add(moveNodeLeftAction);
        moveSubmenu.add(moveNodeRightAction);
        groupsTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            public void mouseClicked(MouseEvent e) {
                TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e
                        .getPoint().y);
                if (path == null)
                    return;
                GroupTreeNode node = (GroupTreeNode) path
                        .getLastPathComponent();
                // the root node is "AllEntries" and cannot be edited
                if (node.isRoot())
                    return;
                if (e.getClickCount() == 2
                        && e.getButton() == MouseEvent.BUTTON1) { // edit
                    editGroupAction.actionPerformed(null); // dummy event
                }
            }
        });
    }

    private void showPopup(MouseEvent e) {
        final TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e
                .getPoint().y);
        addGroupAction.setEnabled(path != null);
        addSubgroupAction.setEnabled(path != null);
        editGroupAction.setEnabled(path != null);
        removeGroupAndSubgroupsAction.setEnabled(path != null);
        removeGroupKeepSubgroupsAction.setEnabled(path != null);
        moveSubmenu.setEnabled(path != null);
        expandSubtreeAction.setEnabled(path != null);
        collapseSubtreeAction.setEnabled(path != null);
        showOverlappingGroupsAction.setEnabled(path != null);
        if (path != null) { // some path dependent enabling/disabling
            groupsTree.setSelectionPath(path);
            GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            AbstractGroup group = node.getGroup();
            if (group instanceof AllEntriesGroup) {
                editGroupAction.setEnabled(false);
                addGroupAction.setEnabled(false);
                removeGroupAndSubgroupsAction.setEnabled(false);
                removeGroupKeepSubgroupsAction.setEnabled(false);
            } else {
                editGroupAction.setEnabled(true);
                addGroupAction.setEnabled(true);
                removeGroupAndSubgroupsAction.setEnabled(true);
                removeGroupKeepSubgroupsAction.setEnabled(true);
            }
            moveNodeUpAction.setEnabled(node.canMoveUp());
            moveNodeDownAction.setEnabled(node.canMoveDown());
            moveNodeLeftAction.setEnabled(node.canMoveLeft());
            moveNodeRightAction.setEnabled(node.canMoveRight());
            moveSubmenu.setEnabled(moveNodeUpAction.isEnabled()
                    || moveNodeDownAction.isEnabled() || moveNodeLeftAction.isEnabled()
                    || moveNodeRightAction.isEnabled());
        }
        groupsContextMenu.show(groupsTree, e.getPoint().x, e.getPoint().y);
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (panel == null) // sorry, we're closed!
            return; // ignore this event
        final TreePath[] selection = groupsTree.getSelectionPaths();
        if (selection == null
                || selection.length == 0
                || (selection.length == 1 && ((GroupTreeNode) selection[0]
                        .getLastPathComponent()).getGroup() instanceof AllEntriesGroup)) {
            panel.stopShowingGroup();
            frame.output(Globals.lang("Displaying no groups") + ".");
            return;
        }
        final AndOrSearchRuleSet searchRules = new AndOrSearchRuleSet(andCb
                .isSelected(), invCb.isSelected());
        int groupMode;
        if (groupModeUnion.isSelected())
            groupMode = GroupTreeNode.GROUP_UNION_CHILDREN;
        else if (groupModeIntersection.isSelected())
            groupMode = GroupTreeNode.GROUP_INTERSECTION_PARENT;
        else
            groupMode = GroupTreeNode.GROUP_ITSELF;

        for (int i = 0; i < selection.length; ++i) {
            searchRules.addRule(((GroupTreeNode) selection[i]
                    .getLastPathComponent()).getSearchRule(groupMode));
        }
        Hashtable searchOptions = new Hashtable();
        searchOptions.put("option", "dummy");
        DatabaseSearch search = new DatabaseSearch(this, searchOptions, searchRules,
                panel, Globals.GROUPSEARCH, floatCb.isSelected(), Globals.prefs
                        .getBoolean("grayOutNonHits"),
                /* true, */select.isSelected());
        search.start();
        frame.output(Globals.lang("Updated group selection") + ".");
    }
    
    /** 
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and set the specified selection and expansion state. */
    public void revalidateGroups(TreePath[] selectionPaths, 
            Enumeration expandedNodes) {
        groupsTreeModel.reload();
        groupsTree.clearSelection();
        if (selectionPaths != null) {
            groupsTree.setSelectionPaths(selectionPaths);
        }
        // tree is completely collapsed here
        if (expandedNodes != null) {
            while (expandedNodes.hasMoreElements())
                groupsTree.expandPath((TreePath)expandedNodes.nextElement());
        }
        groupsTree.revalidate();
    }
    
    /** 
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and maintain the current selection and expansion state. */
    public void revalidateGroups() {
        revalidateGroups(groupsTree.getSelectionPaths(),getExpandedPaths());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refresh) {
            valueChanged(null);
        } else if (e.getSource() == newButton) {
            GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.show();
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                GroupTreeNode newNode = new GroupTreeNode(newGroup);
                groupsRoot.add(newNode);
                revalidateGroups();
                UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        GroupSelector.this, groupsRoot, newNode,
                        UndoableAddOrRemoveGroup.ADD_NODE);
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Created_group_\"%0\".",
                        newGroup.getName()));
            }
        } else if (e.getSource() == autoGroup) {
            AutoGroupDialog gd = new AutoGroupDialog(frame, panel,
                    GroupSelector.this, groupsRoot, Globals.prefs
                            .get("groupsDefaultField"), " .,", ",");
            gd.show();
            // gd does the operation itself
        } else if (e.getSource() instanceof JCheckBox) {
            valueChanged(null);
        }
    }

    public void componentOpening() {
        valueChanged(null);
    }

    public void componentClosing() {
        if (panel != null) // panel may be null if no file is open any more
            panel.stopShowingGroup();
        frame.groupToggle.setSelected(false);
    }

    public void setGroups(GroupTreeNode groupsRoot) {
        groupsTree.setModel(groupsTreeModel = new DefaultTreeModel(groupsRoot));
        this.groupsRoot = groupsRoot;
    }

    /**
     * Adds the specified node as a child of the current root. The group
     * contained in <b>newGroups </b> must not be of type AllEntriesGroup, since
     * every tree has exactly one AllEntriesGroup (its root). The <b>newGroups
     * </b> are inserted directly, i.e. they are not deepCopy()'d.
     */
    public void addGroups(GroupTreeNode newGroups, CompoundEdit ce) {
        // ensure that there are never two instances of AllEntriesGroup
        if (newGroups.getGroup() instanceof AllEntriesGroup)
            return; // JZTODO: output something...
        groupsRoot.add(newGroups);
        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(this,
                groupsRoot, newGroups, UndoableAddOrRemoveGroup.ADD_NODE);
        ce.addEdit(undo);
    }

    /** This action can also be evoked by double-clicking, thus it's a field. */
    final AbstractAction editGroupAction = new AbstractAction(Globals
            .lang("Edit group")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final AbstractGroup oldGroup = node.getGroup();
            final GroupDialog gd = new GroupDialog(frame, panel, oldGroup);
            gd.show();
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                UndoableModifyGroup undo = new UndoableModifyGroup(
                        GroupSelector.this, groupsRoot, node, newGroup);
                node.setGroup(newGroup);
                revalidateGroups();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Modified group \"%0\".",
                        newGroup.getName()));
            }
        }
    };

    final AbstractAction addGroupAction = new AbstractAction(Globals
            .lang("Add Group")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.show();
            if (!gd.okPressed())
                return; // ignore
            final AbstractGroup newGroup = gd.getResultingGroup();
            final GroupTreeNode newNode = new GroupTreeNode(newGroup);
            ((GroupTreeNode) node.getParent()).insert(newNode, node.getParent()
                    .getIndex(node) + 1);
            UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                    GroupSelector.this, groupsRoot, newNode,
                    UndoableAddOrRemoveGroup.ADD_NODE);
            revalidateGroups();
            groupsTree.expandPath(new TreePath(node.getPath()));
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Globals.lang("Added group \"%0\".",
                    newGroup.getName()));
        }
    };

    final AbstractAction addSubgroupAction = new AbstractAction(Globals
            .lang("Add Subgroup")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.show();
            if (!gd.okPressed())
                return; // ignore
            final AbstractGroup newGroup = gd.getResultingGroup();
            final GroupTreeNode newNode = new GroupTreeNode(newGroup);
            node.add(newNode);
            UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                    GroupSelector.this, groupsRoot, newNode,
                    UndoableAddOrRemoveGroup.ADD_NODE);
            revalidateGroups();
            groupsTree.expandPath(new TreePath(node.getPath()));
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Globals.lang("Added group \"%0\".",
                    newGroup.getName()));
        }
    };

    final AbstractAction removeGroupAndSubgroupsAction = new AbstractAction(Globals
            .lang("Remove group and subgroups")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final AbstractGroup group = node.getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("Remove group \"%0\" and its subgroups?",group.getName()), 
                    Globals.lang("Remove group and subgroups"),
                    JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        GroupSelector.this, groupsRoot, node,
                        UndoableAddOrRemoveGroup.REMOVE_NODE_AND_CHILDREN);
                node.removeFromParent();
                revalidateGroups();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Removed group \"%0\" and its subgroups.",
                        group.getName()));
            }
        }
    };

    final AbstractAction removeGroupKeepSubgroupsAction = new AbstractAction(Globals
            .lang("Remove group, keep subgroups")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final AbstractGroup group = node.getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("Remove group \"%0\"?", group.getName()), Globals
                    .lang("Remove group"), JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        GroupSelector.this, groupsRoot, node,
                        UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN);
                final GroupTreeNode parent = (GroupTreeNode) node.getParent();
                final int childIndex = parent.getIndex(node);
                node.removeFromParent();
                while (node.getChildCount() > 0)
                    parent.insert((GroupTreeNode) node.getFirstChild(),
                            childIndex);
                revalidateGroups();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Removed group \"%0\".",
                        group.getName()));
            }
        }
    };
    
    public TreePath getSelectionPath() {
        return groupsTree.getSelectionPath();
    }
    
//  JZTODO lyrics
    public final AbstractAction showOverlappingGroupsAction = new AbstractAction(Globals.lang("Show overlapping groups")) {
        public void actionPerformed(ActionEvent ae) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode selectedNode = (GroupTreeNode) path.getLastPathComponent();
            GroupTreeNode node;
            BibtexEntry entry;
            Vector vec = new Vector();
            final Collection entries = panel.getDatabase().getEntries();
            for (Enumeration e = groupsRoot.depthFirstEnumeration(); e.hasMoreElements(); ) {
                node = (GroupTreeNode) e.nextElement();
                for (Iterator it = entries.iterator(); it.hasNext(); ) {
                    entry = (BibtexEntry) it.next(); 
                    if (node.getGroup().contains(entry) && 
                            selectedNode.getGroup().contains(entry)) {
                        vec.add(node);
                        break;
                    }
                }
            }
            TreeCellRenderer renderer = groupsTree.getCellRenderer();
            if (!(renderer instanceof GroupTreeCellRenderer))
                return; // paranoia
            ((GroupTreeCellRenderer)renderer).setHighlight2Cells(vec.toArray());
        }
    };
    
//  JZTODO lyrics
    public final AbstractAction clearHighlightAction = new AbstractAction(Globals.lang("Clear highlight")) {
        public void actionPerformed(ActionEvent ae) {
            TreeCellRenderer renderer = groupsTree.getCellRenderer();
            if (renderer instanceof GroupTreeCellRenderer) // paranoia
                ((GroupTreeCellRenderer)renderer).setHighlight2Cells(null);
        }
    };
    
    final AbstractAction expandSubtreeAction = new AbstractAction(Globals.lang("Expand subtree")) {
        public void actionPerformed(ActionEvent ae) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            for (Enumeration e = node.depthFirstEnumeration(); e.hasMoreElements(); ) {
                groupsTree.expandPath(new TreePath(
                        ((GroupTreeNode)e.nextElement()).getPath()));
            }
            revalidateGroups();
        }
    };
    
//  JZTODO lyrics
    final AbstractAction collapseSubtreeAction = new AbstractAction(Globals.lang("Collapse subtree")) {
        public void actionPerformed(ActionEvent ae) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            for (Enumeration e = node.depthFirstEnumeration(); e.hasMoreElements(); ) {
                groupsTree.collapsePath(new TreePath(
                        ((GroupTreeNode)e.nextElement()).getPath()));
            }
            revalidateGroups();
        }
    };
    
    final AbstractAction moveNodeUpAction = new AbstractAction(Globals.lang("Up")) {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeUp(node);
        }
    };

    final AbstractAction moveNodeDownAction = new AbstractAction(Globals.lang("Down")) {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeDown(node);
        }
    };

    final AbstractAction moveNodeLeftAction = new AbstractAction(Globals.lang("Left")) {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeLeft(node);
        }
    };

    final AbstractAction moveNodeRightAction = new AbstractAction(Globals.lang("Right")) {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeRight(node);
        }
    };

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeUp(GroupTreeNode node) {
        if (groupsTree.getSelectionCount() != 1) {
            frame.output(Globals.lang("Please select exactly one group to move."));
            return false; // not possible
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveUp() || (undo = node.moveUp(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" up.", node.getGroup().getName()));
            return false; // not possible
        }
        // update of selection/expansion state not required
        // when moving amongst siblings (no path is invalidated)
        revalidateGroups();
        concludeMoveGroup(undo, node);
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeDown(GroupTreeNode node) {
        if (groupsTree.getSelectionCount() != 1) {
            frame.output(Globals.lang("Please select exactly one group to move."));
            return false; // not possible
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveDown() || (undo = node.moveDown(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" down.", node.getGroup().getName()));
            return false; // not possible
        }
        // update of selection/expansion state not required
        // when moving amongst siblings (no path is invalidated)
        revalidateGroups();
        concludeMoveGroup(undo, node);
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeLeft(GroupTreeNode node) {
        if (groupsTree.getSelectionCount() != 1) {
            frame.output(Globals.lang("Please select exactly one group to move."));
            return false; // not possible
        }
        AbstractUndoableEdit undo = null;
        Enumeration expandedPaths = getExpandedPaths();
        if (!node.canMoveLeft() || (undo = node.moveLeft(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" left.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(new TreePath[]{new TreePath(node.getPath())},
                groupsTree.refreshPaths(expandedPaths));
        concludeMoveGroup(undo, node);
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeRight(GroupTreeNode node) {
        if (groupsTree.getSelectionCount() != 1) {
            frame.output(Globals.lang("Please select exactly one group to move."));
            return false; // not possible
        }
        AbstractUndoableEdit undo = null;
        Enumeration expandedPaths = getExpandedPaths();
        if (!node.canMoveRight() || (undo = node.moveRight(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" right.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(new TreePath[]{new TreePath(node.getPath())},
                groupsTree.refreshPaths(expandedPaths));
        concludeMoveGroup(undo, node);
        return true;
    }
    
    /**
     * Concludes the moving of a group tree node by storing the specified
     * undo information, marking the change, and setting the status line.
     * @param undo Undo information for the move operation. 
     * @param node The node that has been moved.
     */
    public void concludeMoveGroup(AbstractUndoableEdit undo, GroupTreeNode node) {
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        frame.output(Globals.lang("Moved group \"%0\".", 
                node.getGroup().getName()));
    }
    
    public void concludeAssignment(AbstractUndoableEdit undo, GroupTreeNode node, int assignedEntries) {
        if (undo == null) {
            frame.output(Globals.lang("The group \"%0\" already contains the selection.",
                    new String[]{node.getGroup().getName()}));
            return;
        }
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        final String groupName = node.getGroup().getName();
        if (assignedEntries == 1)
            frame.output(Globals.lang("Assigned 1 entry to group \"%0\".", groupName));
        else
            frame.output(Globals.lang("Assigned %0 entries to group \"%1\".", 
                    String.valueOf(assignedEntries), groupName));
    }
    
    JMenu moveSubmenu = new JMenu(Globals.lang("Move"));

    public GroupTreeNode getGroupTreeRoot() {
        return groupsRoot;
    }
    
    public Enumeration getExpandedPaths() {
        return groupsTree.getExpandedDescendants(
                new TreePath(groupsRoot.getPath()));
    }

    /** panel may be null to indicate that no file is currently open. */
    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel == null) { // hide groups
            frame.sidePaneManager.ensureNotVisible("groups");
            return;
        }
        MetaData metaData = panel.metaData();
        if (metaData.getGroups() != null) {
            setGroups(metaData.getGroups());
            if (!groupsRoot.isLeaf()) { // groups were defined
                frame.sidePaneManager.ensureVisible("groups");
                frame.groupToggle.setSelected(true);
            }
        }
        else {
            GroupTreeNode newGroupsRoot = new GroupTreeNode(new AllEntriesGroup());
            metaData.setGroups(newGroupsRoot);
            setGroups(newGroupsRoot);
         }
        validateTree();
    }


    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage) {
        // this should never happen, since regular expressions are checked for
        // correctness by the edit group dialog, and no other errors should
        // occur in a search
        System.err.println("Error in group search: "+errorMessage
                + ". Please report this on www.sf.net/projects/jabref");
    }

    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage, Exception exception) {
        reportError(errorMessage);
    }
    
    public void showMatchingGroups(BibtexEntry[] entries, boolean requireAll) {
        GroupTreeNode node;
        AbstractGroup group;
        Vector vec = new Vector();
        for (Enumeration e = groupsRoot.preorderEnumeration(); e.hasMoreElements(); ) {
            node = (GroupTreeNode) e.nextElement();
            group = node.getGroup();
            for (int i = 0; i < entries.length; ++i) {
                if (requireAll) {
                    if (!group.contains(entries[i])) {
                        System.out.println("NOT adding node: " + group.getName());
                        break;
                    }
                } else {
                    if (group.contains(entries[i])) {
                        System.out.println("adding node: " + group.getName());
                        vec.add(node);
                    }
                } 
            }
        }
        TreeCellRenderer renderer = groupsTree.getCellRenderer();
        if (!(renderer instanceof GroupTreeCellRenderer))
            return; // paranoia
        ((GroupTreeCellRenderer)renderer).setHighlight2Cells(vec.toArray());
        // ensure that all highlighted nodes are visible
        for (int i = 0; i < vec.size(); ++i) {
            node = (GroupTreeNode)((GroupTreeNode)vec.elementAt(i)).getParent();
            if (node != null)
                groupsTree.expandPath(new TreePath(node.getPath()));
        }
        groupsTree.revalidate();
    }
}

