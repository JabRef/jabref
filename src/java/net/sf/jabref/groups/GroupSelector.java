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
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.undo.*;

import net.sf.jabref.*;

public class GroupSelector extends SidePaneComponent implements
        TreeSelectionListener, ActionListener {
    /*
     * Groups are stored in the vector like the following: field1, name1,
     * regexp1, field2, name2, regexp2, ...
     */
    JButton newButton = new JButton(new ImageIcon(GUIGlobals.newSmallIconFile)),
            helpButton = new JButton(
                    new ImageIcon(GUIGlobals.helpSmallIconFile)),
            refresh = new JButton(
                    new ImageIcon(GUIGlobals.refreshSmallIconFile)),
            autoGroup = new JButton(new ImageIcon(GUIGlobals.autoGroupIcon)),
            openset = new JButton(Globals.lang("Settings"));
    Color bgColor = Color.white;
    JTree groupsTree;
    DefaultTreeModel groupsTreeModel;
    final GroupTreeNode groupsRoot;
    JScrollPane sp;
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JabRefFrame frame;
    BasePanel panel;
    String searchField;
    JPopupMenu groupsContextMenu = new JPopupMenu();
    JPopupMenu settings = new JPopupMenu();
    private JRadioButtonMenuItem hideNonHits, grayOut;
    JRadioButtonMenuItem groupModeUnion = new JRadioButtonMenuItem(Globals
            .lang("Groups: Include Subgroups"), true);
    JRadioButtonMenuItem groupModeIntersection = new JRadioButtonMenuItem(
            Globals.lang("Groups: Intersection with Supergroups"), false);
    JRadioButtonMenuItem groupModeIndependent = new JRadioButtonMenuItem(
            Globals.lang("Groups: Independent"), false);
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
    JabRefPreferences prefs;

    /**
     * The first element for each group defines which field to use for the
     * quicksearch. The next two define the name and regexp for the group.
     * 
     * @param groupData
     *            The group meta data in raw format.
     */
    public GroupSelector(JabRefFrame frame, BasePanel panel,
            GroupTreeNode groupsRoot, SidePaneManager manager,
            JabRefPreferences prefs) {
        super(manager);
        this.prefs = prefs;
        this.groupsRoot = groupsRoot;
        final JabRefPreferences _prefs = prefs;
        final BasePanel basePanel = panel;
        this.manager = manager;
        this.frame = frame;
        this.panel = panel;
        hideNonHits = new JRadioButtonMenuItem(Globals.lang("Hide non-hits"),
                !prefs.getBoolean("grayOutNonHits"));
        grayOut = new JRadioButtonMenuItem(Globals.lang("Gray out non-hits"),
                prefs.getBoolean("grayOutNonHits"));
        nonHits.add(hideNonHits);
        nonHits.add(grayOut);
        floatCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                _prefs.putBoolean("groupFloatSelections", floatCb.isSelected());
            }
        });
        andCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                _prefs.putBoolean("groupIntersectSelections", andCb
                        .isSelected());
            }
        });
        invCb.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                _prefs.putBoolean("groupInvertSelections", invCb.isSelected());
            }
        });
        select.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                _prefs.putBoolean("groupSelectMatches", select.isSelected());
            }
        });
        grayOut.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                _prefs.putBoolean("grayOutNonHits", grayOut.isSelected());
            }
        });
        if (prefs.getBoolean("groupFloatSelections")) {
            floatCb.setSelected(true);
            highlCb.setSelected(false);
        } else {
            highlCb.setSelected(true);
            floatCb.setSelected(false);
        }
        if (prefs.getBoolean("groupIntersectSelections")) {
            andCb.setSelected(true);
            orCb.setSelected(false);
        } else {
            orCb.setSelected(true);
            andCb.setSelected(false);
        }
        invCb.setSelected(prefs.getBoolean("groupInvertSelections"));
        select.setSelected(prefs.getBoolean("groupSelectMatches"));
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
                int i = _prefs.getInt("groupsVisibleRows") + 1;
                groupsTree.setVisibleRowCount(i);
                groupsTree.revalidate();
                groupsTree.repaint();
                GroupSelector.this.revalidate();
                GroupSelector.this.repaint();
                _prefs.putInt("groupsVisibleRows", i);
            }
        });
        reduce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = _prefs.getInt("groupsVisibleRows") - 1;
                if (i < 1)
                    i = 1;
                groupsTree.setVisibleRowCount(i);
                groupsTree.revalidate();
                groupsTree.repaint();
                GroupSelector.this.revalidate();
                // _panel.sidePaneManager.revalidate();
                GroupSelector.this.repaint();
                _prefs.putInt("groupsVisibleRows", i);
            }
        });
        Dimension butDim = new Dimension(20, 20);
        Dimension butDim2 = new Dimension(40, 20);
        // newButton.setPreferredSize(butDim);
        // newButton.setMinimumSize(butDim);
        // refresh.setPreferredSize(butDim);
        // refresh.setMinimumSize(butDim);
        // helpButton.setPreferredSize(butDim);
        // helpButton.setMinimumSize(butDim);
        // autoGroup.setPreferredSize(butDim);
        // autoGroup.setMinimumSize(butDim);
        openset.setPreferredSize(butDim2);
        // openset.setMinimumSize(butDim2);
        expand.setPreferredSize(butDim);
        // expand.setMinimumSize(butDim);
        reduce.setPreferredSize(butDim);
        // reduce.setMinimumSize(butDim);
        Insets butIns = new Insets(0, 0, 0, 0);
        // helpButton.setMargin(butIns);
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
        setLayout(gbl);
        SidePaneHeader header = new SidePaneHeader("Groups",
                GUIGlobals.groupsIconFile, this);
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.fill = GridBagConstraints.BOTH;
        con.insets = new Insets(0, 0, 2, 0);
        con.weightx = 1;
        gbl.setConstraints(header, con);
        add(header);
        con.gridwidth = 1;
        con.insets = new Insets(0, 0, 0, 0);
        gbl.setConstraints(newButton, con);
        add(newButton);
        gbl.setConstraints(refresh, con);
        add(refresh);
        gbl.setConstraints(autoGroup, con);
        add(autoGroup);
        con.gridwidth = GridBagConstraints.REMAINDER;
        HelpAction helpAction = new HelpAction(frame.helpDiag,
                GUIGlobals.groupsHelp, "Help on groups");
        helpButton.addActionListener(helpAction);
        helpButton.setToolTipText(Globals.lang("Help on groups"));
        gbl.setConstraints(helpButton, con);
        add(helpButton);
        // header.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        // helpButton.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        groupsTree = new JTree();
        groupsTree.setShowsRootHandles(true);
        groupsTree.setCellRenderer(new GroupTreeCellRenderer());
        groupsTree.setToggleClickCount(0);
        groupsTree.addTreeSelectionListener(this);
        ToolTipManager.sharedInstance().registerComponent(groupsTree);
        // JZTODO JZPUWIL: drag and drop...
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                groupsTree, DnDConstants.ACTION_MOVE,
                new DragGestureListener() {
                    public void dragGestureRecognized(DragGestureEvent dge) {
                        // TODO Auto-generated method stub
                        System.out.println(dge);
                    }
                });
        groupsTree.setShowsRootHandles(false);
        // groupsTree.setPrototypeCellValue("Suitable length");
        // // The line above decides on the list's preferred width.
        groupsTree.setVisibleRowCount(prefs.getInt("groupsVisibleRows"));
        groupsTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        groupsTree.setModel(groupsTreeModel = new DefaultTreeModel(groupsRoot));
        sp = new JScrollPane(groupsTree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        revalidateGroups();
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weighty = 1;
        gbl.setConstraints(sp, con);
        add(sp);
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
        add(pan);
        definePopup();
    }

    private void definePopup() {
        groupsContextMenu.add(addGroupAction);
        groupsContextMenu.add(addSubgroupAction);
        groupsContextMenu.add(editGroupAction);
        groupsContextMenu.add(removeGroupAndSubgroupsAction);
        groupsContextMenu.add(removeGroupKeepSubgroupsAction);
        groupsContextMenu.add(moveSubmenu);
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
        TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e
                .getPoint().y);
        if (path == null)
            return;
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
        groupsContextMenu.show(groupsTree, e.getPoint().x, e.getPoint().y);
    }

    public void valueChanged(TreeSelectionEvent e) {
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
        DatabaseSearch search = new DatabaseSearch(searchOptions, searchRules,
                panel, Globals.GROUPSEARCH, floatCb.isSelected(), Globals.prefs
                        .getBoolean("grayOutNonHits"),
                /* true, */select.isSelected());
        search.start();
        frame.output(Globals.lang("Updated group selection") + ".");
    }

    public void revalidateGroups() {
        groupsTreeModel.reload();
        groupsTree.clearSelection();
        groupsTree.revalidate();
    }

    /**
     * @param paths
     *            The paths to select.
     */
    public void revalidateGroups(TreePath[] paths) {
        groupsTreeModel.reload();
        groupsTree.setSelectionPaths(paths);
        groupsTree.revalidate();
    }

    /**
     * @param path
     *            The path to select.
     */
    public void revalidateGroups(TreePath path) {
        revalidateGroups(new TreePath[] { path });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refresh) {
            valueChanged(null); // JZTODO: null OK?
        } else if (e.getSource() == newButton) {
            GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.show();
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                GroupTreeNode newNode = new GroupTreeNode(newGroup);
                groupsRoot.add(newNode);
                revalidateGroups(groupsTree.getSelectionPaths());
                UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        GroupSelector.this, groupsRoot, newNode,
                        UndoableAddOrRemoveGroup.ADD_NODE);
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Created group") + " '"
                        + newGroup.getName() + "'.");
            }
        } else if (e.getSource() == autoGroup) {
            AutoGroupDialog gd = new AutoGroupDialog(frame, panel,
                    GroupSelector.this, groupsRoot, prefs
                            .get("groupsDefaultField"), " .,", ",");
            gd.show();
            // gd does the operation itself
        } else if (e.getSource() instanceof JCheckBox) {
            valueChanged(null); // JZTODO: null OK?
        }
    }

    public void componentOpening() {
        valueChanged(null); // JZTODO: null OK?
    }

    public void componentClosing() {
        panel.stopShowingGroup();
        frame.groupToggle.setSelected(false);
    }

    public void setGroups(GroupTreeNode groupsRoot) {
        groupsTree.setModel(groupsTreeModel = new DefaultTreeModel(groupsRoot));
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
    AbstractAction editGroupAction = new AbstractAction(Globals
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
                revalidateGroups(new TreePath(node.getPath()));
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Modified group") + " '"
                        + newGroup.getName() + "'.");
            }
        }
    };

    AbstractAction addGroupAction = new AbstractAction(Globals
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
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output("Added group '" + newGroup.getName() + "'.");
        }
    };

    AbstractAction addSubgroupAction = new AbstractAction(Globals
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
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output("Added group '" + newGroup.getName() + "'.");
        }
    };

    AbstractAction removeGroupAndSubgroupsAction = new AbstractAction(Globals
            .lang("Remove group and subgroups")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final AbstractGroup group = node.getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("Remove group")
                    + " '" + group.getName() + "' and its subgroups?", Globals
                    .lang("Remove group and subgroups"),
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
                frame.output(Globals.lang("Removed group") + " '"
                        + group.getName() + "' and its subgroups.");
            }
        }
    };

    AbstractAction removeGroupKeepSubgroupsAction = new AbstractAction(Globals
            .lang("Remove group, keep subgroups")) {
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = (GroupTreeNode) groupsTree
                    .getSelectionPath().getLastPathComponent();
            final AbstractGroup group = node.getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("Remove group")
                    + " '" + group.getName() + "'?", Globals
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
                frame.output(Globals.lang("Removed group") + " '"
                        + group.getName() + "' and its subgroups.");
            }
        }
    };
    
    public TreePath getSelectionPath() {
        return groupsTree.getSelectionPath();
    }

    AbstractAction moveNodeUpAction = new AbstractAction("Up") {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeUp(node);
        }
    };

    AbstractAction moveNodeDownAction = new AbstractAction("Down") {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeDown(node);
        }
    };

    AbstractAction moveNodeLeftAction = new AbstractAction("Left") {
        public void actionPerformed(ActionEvent e) {
            final TreePath path = getSelectionPath();
            if (path == null)
                return;
            final GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            moveNodeLeft(node);
        }
    };

    AbstractAction moveNodeRightAction = new AbstractAction("Right") {
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
        AbstractUndoableEdit undo = null;
        if (!node.canMoveUp() || (undo = node.moveUp(GroupSelector.this)) == null) {
            frame.output(Globals.lang("Cannot move group") + " '"
                    + node.getGroup().getName() + "' up.");
            return false; // not possible
        }
        revalidateGroups(new TreePath(node.getPath()));
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        frame.output(Globals.lang("Moved group") + " '"
                + node.getGroup().getName() + "'.");
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeDown(GroupTreeNode node) {
        AbstractUndoableEdit undo = null;
        if (!node.canMoveDown() || (undo = node.moveDown(GroupSelector.this)) == null) {
            frame.output(Globals.lang("Cannot move group") + " '"
                    + node.getGroup().getName() + "' down.");
            return false; // not possible
        }
        revalidateGroups(new TreePath(node.getPath()));
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        frame.output(Globals.lang("Moved group") + " '"
                + node.getGroup().getName() + "'.");
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeLeft(GroupTreeNode node) {
        AbstractUndoableEdit undo = null;
        if (!node.canMoveLeft() || (undo = node.moveLeft(GroupSelector.this)) == null) {
            frame.output(Globals.lang("Cannot move group") + " '"
                    + node.getGroup().getName() + "' left.");
            return false; // not possible
        }
        revalidateGroups(new TreePath(node.getPath()));
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        frame.output(Globals.lang("Moved group") + " '"
                + node.getGroup().getName() + "'.");
        return true;
    }
    
    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeRight(GroupTreeNode node) {
        AbstractUndoableEdit undo = null;
        if (!node.canMoveRight() || (undo = node.moveRight(GroupSelector.this)) == null) {
            frame.output(Globals.lang("Cannot move group") + " '"
                    + node.getGroup().getName() + "' right.");
            return false; // not possible
        }
        revalidateGroups(new TreePath(node.getPath()));
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        frame.output(Globals.lang("Moved group") + " '"
                + node.getGroup().getName() + "'.");
        return true;
    }
    
    JMenu moveSubmenu = new JMenu("Move");

    public GroupTreeNode getGroupTreeRoot() {
        return groupsRoot;
    }
}
