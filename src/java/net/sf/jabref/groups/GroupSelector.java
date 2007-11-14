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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.ErrorMessageDisplay;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.HelpAction;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.SearchRule;
import net.sf.jabref.SearchRuleSet;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.undo.NamedCompound;

public class GroupSelector extends SidePaneComponent implements
        TreeSelectionListener, ActionListener, ErrorMessageDisplay {
    JButton newButton = new JButton(GUIGlobals.getImage("new")),
            helpButton = new JButton(
                    GUIGlobals.getImage("help")),
            refresh = new JButton(
                    GUIGlobals.getImage("refresh")),
            autoGroup = new JButton(GUIGlobals.getImage("autoGroup")),
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
    JCheckBoxMenuItem showOverlappingGroups = new JCheckBoxMenuItem(
                    Globals.lang("Highlight overlapping groups")); // JZTODO lyrics
    ButtonGroup bgr = new ButtonGroup();
    ButtonGroup visMode = new ButtonGroup();
    ButtonGroup nonHits = new ButtonGroup();
    JButton expand = new JButton(GUIGlobals.getImage("down")),
            reduce = new JButton(GUIGlobals.getImage("up"));
    SidePaneManager manager;


    /**
     * The first element for each group defines which field to use for the
     * quicksearch. The next two define the name and regexp for the group.
     *
     *
     */
    public GroupSelector(JabRefFrame frame, SidePaneManager manager) {
        super(manager, GUIGlobals.getIconUrl("toggleGroups"), Globals.lang("Groups"));
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
        showOverlappingGroups.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean("groupShowOverlapping",
                                showOverlappingGroups.isSelected());
                if (!showOverlappingGroups.isSelected())
                    groupsTree.setHighlight2Cells(null);
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

        invCb.setSelected(Globals.prefs.getBoolean("groupInvertSelections"));
        showOverlappingGroups.setSelected(Globals.prefs.getBoolean("groupShowOverlapping"));
        select.setSelected(Globals.prefs.getBoolean("groupSelectMatches"));

        openset.setMargin(new Insets(0, 0, 0, 0));
        settings.add(andCb);
        settings.add(orCb);
        settings.addSeparator();
        settings.add(invCb);
        settings.addSeparator();
        //settings.add(highlCb);
        //settings.add(floatCb);
        //settings.addSeparator();
        settings.add(select);
        settings.addSeparator();
        settings.add(grayOut);
        settings.add(hideNonHits);
        settings.addSeparator();
        settings.add(showOverlappingGroups);

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
                System.out.println(GroupSelector.this.getHeight());
                System.out.println(GroupSelector.this.getPreferredSize().getHeight());
                
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

        int butSize = newButton.getIcon().getIconHeight() + 5;
        Dimension butDim = new Dimension(butSize, butSize);
        //Dimension butDimSmall = new Dimension(20, 20);

        newButton.setPreferredSize(butDim);
        newButton.setMinimumSize(butDim);
        refresh.setPreferredSize(butDim);
        refresh.setMinimumSize(butDim);
        helpButton.setPreferredSize(butDim);
        helpButton.setMinimumSize(butDim);
        autoGroup.setPreferredSize(butDim);
        autoGroup.setMinimumSize(butDim);
        openset.setPreferredSize(butDim);
        openset.setMinimumSize(butDim);
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
        showOverlappingGroups.addActionListener(this);
        autoGroup.addActionListener(this);
        floatCb.addActionListener(this);
        highlCb.addActionListener(this);
        select.addActionListener(this);
        hideNonHits.addActionListener(this);
        grayOut.addActionListener(this);
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
        showOverlappingGroups.setToolTipText( // JZTODO lyrics
                        "Highlight groups that contain entries contained in any currently selected group");
        floatCb.setToolTipText(Globals
                .lang("Move entries in group selection to the top"));
        highlCb.setToolTipText(Globals
                .lang("Gray out entries not in group selection"));
        select
                .setToolTipText(Globals
                        .lang("Select entries in group selection"));
        expand.setToolTipText(Globals.lang("Show one more row"));
        reduce.setToolTipText(Globals.lang("Show one less rows"));
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
        moveNodeUpAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
        moveNodeDownAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
        moveNodeLeftAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
        moveNodeRightAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));
    }

    private void definePopup() {
        // These key bindings are just to have the shortcuts displayed
        // in the popup menu. The actual keystroke processing is in
        // BasePanel (entryTable.addKeyListener(...)).
        groupsContextMenu.add(editGroupPopupAction);
        groupsContextMenu.add(addGroupPopupAction);
        groupsContextMenu.add(addSubgroupPopupAction);
        groupsContextMenu.addSeparator();
        groupsContextMenu.add(removeGroupAndSubgroupsPopupAction);
        groupsContextMenu.add(removeGroupKeepSubgroupsPopupAction);
        groupsContextMenu.add(removeSubgroupsPopupAction);
        groupsContextMenu.addSeparator();
        groupsContextMenu.add(expandSubtreePopupAction);
        groupsContextMenu.add(collapseSubtreePopupAction);
        groupsContextMenu.addSeparator();
        groupsContextMenu.add(moveSubmenu);
        sortSubmenu.add(sortDirectSubgroupsPopupAction);
        sortSubmenu.add(sortAllSubgroupsPopupAction);
        groupsContextMenu.add(sortSubmenu);
        moveSubmenu.add(moveNodeUpPopupAction);
        moveSubmenu.add(moveNodeDownPopupAction);
        moveSubmenu.add(moveNodeLeftPopupAction);
        moveSubmenu.add(moveNodeRightPopupAction);
        groupsContextMenu.addSeparator();
        groupsContextMenu.add(addToGroup);
        groupsContextMenu.add(moveToGroup);
        groupsContextMenu.add(removeFromGroup);
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
        // be sure to remove a possible border highlight when the popup menu
        // disappears
        groupsContextMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // nothing to do
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                groupsTree.setHighlightBorderCell(null);
            }
            public void popupMenuCanceled(PopupMenuEvent e) {
                groupsTree.setHighlightBorderCell(null);
            }
        });
    }

    private void showPopup(MouseEvent e) {
        final TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e
                .getPoint().y);
        addGroupPopupAction.setEnabled(true);
        addSubgroupPopupAction.setEnabled(path != null);
        editGroupPopupAction.setEnabled(path != null);
        removeGroupAndSubgroupsPopupAction.setEnabled(path != null);
        removeGroupKeepSubgroupsPopupAction.setEnabled(path != null);
        moveSubmenu.setEnabled(path != null);
        expandSubtreePopupAction.setEnabled(path != null);
        collapseSubtreePopupAction.setEnabled(path != null);
        removeSubgroupsPopupAction.setEnabled(path != null);
        sortSubmenu.setEnabled(path != null);
        addToGroup.setEnabled(false);
        moveToGroup.setEnabled(false);
        removeFromGroup.setEnabled(false);
        if (path != null) { // some path dependent enabling/disabling
            GroupTreeNode node = (GroupTreeNode) path.getLastPathComponent();
            editGroupPopupAction.setNode(node);
            addSubgroupPopupAction.setNode(node);
            removeGroupAndSubgroupsPopupAction.setNode(node);
            removeSubgroupsPopupAction.setNode(node);
            removeGroupKeepSubgroupsPopupAction.setNode(node);
            expandSubtreePopupAction.setNode(node);
            collapseSubtreePopupAction.setNode(node);
            sortDirectSubgroupsPopupAction.setNode(node);
            sortAllSubgroupsPopupAction.setNode(node);
            groupsTree.setHighlightBorderCell(node);
            AbstractGroup group = node.getGroup();
            if (group instanceof AllEntriesGroup) {
                editGroupPopupAction.setEnabled(false);
                addGroupPopupAction.setEnabled(false);
                removeGroupAndSubgroupsPopupAction.setEnabled(false);
                removeGroupKeepSubgroupsPopupAction.setEnabled(false);
            } else {
                editGroupPopupAction.setEnabled(true);
                addGroupPopupAction.setEnabled(true);
                addGroupPopupAction.setNode(node);
                removeGroupAndSubgroupsPopupAction.setEnabled(true);
                removeGroupKeepSubgroupsPopupAction.setEnabled(true);
            }
                        expandSubtreePopupAction.setEnabled(groupsTree.isCollapsed(path) ||
                                        groupsTree.hasCollapsedDescendant(path));
                        collapseSubtreePopupAction.setEnabled(groupsTree.isExpanded(path) ||
                                        groupsTree.hasExpandedDescendant(path));
            sortSubmenu.setEnabled(!node.isLeaf());
            removeSubgroupsPopupAction.setEnabled(!node.isLeaf());
            moveNodeUpPopupAction.setEnabled(node.canMoveUp());
            moveNodeDownPopupAction.setEnabled(node.canMoveDown());
            moveNodeLeftPopupAction.setEnabled(node.canMoveLeft());
            moveNodeRightPopupAction.setEnabled(node.canMoveRight());
            moveSubmenu.setEnabled(moveNodeUpPopupAction.isEnabled()
                    || moveNodeDownPopupAction.isEnabled()
                    || moveNodeLeftPopupAction.isEnabled()
                    || moveNodeRightPopupAction.isEnabled());
            moveNodeUpPopupAction.setNode(node);
            moveNodeDownPopupAction.setNode(node);
            moveNodeLeftPopupAction.setNode(node);
            moveNodeRightPopupAction.setNode(node);
            // add/remove entries to/from group
            BibtexEntry[] selection = frame.basePanel().getSelectedEntries();
            if (selection.length > 0) {
                if (node.getGroup().supportsAdd() && !node.getGroup().
                        containsAll(selection)) {
                    addToGroup.setNode(node);
                    addToGroup.setBasePanel(panel);
                    addToGroup.setEnabled(true);
                    moveToGroup.setNode(node);
                    moveToGroup.setBasePanel(panel);
                    moveToGroup.setEnabled(true);
                }
                if (node.getGroup().supportsRemove() && node.getGroup().
                        containsAny(selection)) {
                    removeFromGroup.setNode(node);
                    removeFromGroup.setBasePanel(panel);
                    removeFromGroup.setEnabled(true);
                }
            }
        } else {
            editGroupPopupAction.setNode(null);
            addGroupPopupAction.setNode(null);
            addSubgroupPopupAction.setNode(null);
            removeGroupAndSubgroupsPopupAction.setNode(null);
            removeSubgroupsPopupAction.setNode(null);
            removeGroupKeepSubgroupsPopupAction.setNode(null);
            moveNodeUpPopupAction.setNode(null);
            moveNodeDownPopupAction.setNode(null);
            moveNodeLeftPopupAction.setNode(null);
            moveNodeRightPopupAction.setNode(null);
            expandSubtreePopupAction.setNode(null);
            collapseSubtreePopupAction.setNode(null);
            sortDirectSubgroupsPopupAction.setNode(null);
            sortAllSubgroupsPopupAction.setNode(null);
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
            panel.mainTable.stopShowingFloatGrouping();
            if (showOverlappingGroups.isSelected())
                groupsTree.setHighlight2Cells(null);
            frame.output(Globals.lang("Displaying no groups") + ".");
            return;
        }
        final AndOrSearchRuleSet searchRules = new AndOrSearchRuleSet(andCb
                .isSelected(), invCb.isSelected());

        for (int i = 0; i < selection.length; ++i) {
                        searchRules.addRule(((GroupTreeNode) selection[i]
                                        .getLastPathComponent()).getSearchRule());
                }
        Hashtable<String, String> searchOptions = new Hashtable<String, String>();
        searchOptions.put("option", "dummy");
        GroupingWorker worker = new GroupingWorker(searchRules, searchOptions);
        worker.getWorker().run();
        worker.getCallBack().update();
        /*panel.setGroupMatcher(new SearchMatcher(searchRules, searchOptions));
        DatabaseSearch search = new DatabaseSearch(this, searchOptions, searchRules,
                panel, Globals.GROUPSEARCH, floatCb.isSelected(), Globals.prefs
                        .getBoolean("grayOutNonHits"),
                //true,
                select.isSelected());
        search.start();*/


    }

    class GroupingWorker extends AbstractWorker {
        private SearchRuleSet rules;
        private Hashtable<String, String> searchTerm;
        private ArrayList<BibtexEntry> matches = new ArrayList<BibtexEntry>();
        private boolean showOverlappingGroupsP;
        int hits = 0;

        public GroupingWorker(SearchRuleSet rules, Hashtable<String, String> searchTerm) {
            this.rules = rules;
            this.searchTerm = searchTerm;
            showOverlappingGroupsP = showOverlappingGroups.isSelected();
        }

        public void run() {
            for (BibtexEntry entry : panel.getDatabase().getEntries()){
                boolean hit = rules.applyRule(searchTerm, entry) > 0;
                entry.setGroupHit(hit);
                if (hit) {
                    hits++;
                    if (showOverlappingGroupsP)
                        matches.add(entry);
                }
            }
        }

        public void update() {
            // Show the result in the chosen way:
            if (hideNonHits.isSelected()) {
                panel.mainTable.stopShowingFloatGrouping(); // Turn off shading, if active.
                panel.setGroupMatcher(GroupMatcher.INSTANCE); // Turn on filtering.

            }
            else if (grayOut.isSelected()) {
                panel.stopShowingGroup(); // Turn off filtering, if active.
                panel.mainTable.showFloatGrouping(GroupMatcher.INSTANCE); // Turn on shading.
            }

            if (showOverlappingGroupsP) {
                                showOverlappingGroups(matches);
        }
            frame.output(Globals.lang("Updated group selection") + ".");
        }
    }
    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and set the specified selection and expansion state.
     * @param node If this is non-null, the view is scrolled to make it visible.
     */
    public void revalidateGroups(TreePath[] selectionPaths,
            Enumeration<TreePath> expandedNodes) {
            revalidateGroups(selectionPaths, expandedNodes, null);
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and set the specified selection and expansion state.
     * @param node If this is non-null, the view is scrolled to make it visible.
     */
    public void revalidateGroups(TreePath[] selectionPaths,
            Enumeration<TreePath> expandedNodes, GroupTreeNode node) {
        groupsTreeModel.reload();
        groupsTree.clearSelection();
        if (selectionPaths != null) {
            groupsTree.setSelectionPaths(selectionPaths);
        }
        // tree is completely collapsed here
        if (expandedNodes != null) {
            while (expandedNodes.hasMoreElements())
                groupsTree.expandPath(expandedNodes.nextElement());
        }
        groupsTree.revalidate();
        if (node != null) {
                groupsTree.scrollPathToVisible(new TreePath(node.getPath()));
        }
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and maintain the current selection and expansion state. */
    public void revalidateGroups() {
        revalidateGroups(null);
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has
     * been changed) and maintain the current selection and expansion state.
     * @param node If this is non-null, the view is scrolled to make it visible.
     */
    public void revalidateGroups(GroupTreeNode node) {
        revalidateGroups(groupsTree.getSelectionPaths(),getExpandedPaths(),node);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refresh) {
            valueChanged(null);
        } else if (e.getSource() == newButton) {
            GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true); // gd.show(); -> deprecated since 1.5
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
            gd.setVisible(true); // gd.show(); -> deprecated since 1.5
            // gd does the operation itself
        } else if (e.getSource() instanceof JCheckBox) {
            valueChanged(null);
        } else if (e.getSource() instanceof JCheckBoxMenuItem) {
            valueChanged(null);
        } else if (e.getSource() instanceof JRadioButtonMenuItem) {
            valueChanged(null);
        }
    }

    public void componentOpening() {
        valueChanged(null);
    }

    public void componentClosing() {
        if (panel != null) {// panel may be null if no file is open any more
            panel.stopShowingGroup();
            panel.mainTable.stopShowingFloatGrouping();
        }
        frame.groupToggle.setSelected(false);
    }

    public void setGroups(GroupTreeNode groupsRoot) {
        groupsTree.setModel(groupsTreeModel = new DefaultTreeModel(groupsRoot));
        this.groupsRoot = groupsRoot;
        if (Globals.prefs.getBoolean("groupExpandTree"))
                groupsTree.expandSubtree(groupsRoot);
    }

    /**
     * Adds the specified node as a child of the current root. The group
     * contained in <b>newGroups </b> must not be of type AllEntriesGroup, since
     * every tree has exactly one AllEntriesGroup (its root). The <b>newGroups
     * </b> are inserted directly, i.e. they are not deepCopy()'d.
     */
    public void addGroups(GroupTreeNode newGroups, CompoundEdit ce) {
        // paranoia: ensure that there are never two instances of AllEntriesGroup
        if (newGroups.getGroup() instanceof AllEntriesGroup)
            return; // this should be impossible anyway
        groupsRoot.add(newGroups);
        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(this,
                groupsRoot, newGroups, UndoableAddOrRemoveGroup.ADD_NODE);
        ce.addEdit(undo);
    }

    private abstract class NodeAction extends AbstractAction {
        protected GroupTreeNode m_node = null;
        public NodeAction(String s) {
            super(s);
        }
        public GroupTreeNode getNode() {
            return m_node;
        }
        public void setNode(GroupTreeNode node) {
            this.m_node = node;
        }
        /** Returns the node to use in this action. If a node has been
         * set explicitly (via setNode), it is returned. Otherwise, the first
         * node in the current selection is returned. If all this fails, null
         * is returned. */
        public GroupTreeNode getNodeToUse() {
            if (m_node != null)
                return m_node;
            TreePath path = groupsTree.getSelectionPath();
            if (path != null)
                return (GroupTreeNode) path.getLastPathComponent();
            return null;
        }
    }

    final AbstractAction editGroupAction = new EditGroupAction();
    final NodeAction editGroupPopupAction = new EditGroupAction();
    final NodeAction addGroupPopupAction = new AddGroupAction();
    final NodeAction addSubgroupPopupAction = new AddSubgroupAction();
    final NodeAction removeGroupAndSubgroupsPopupAction = new RemoveGroupAndSubgroupsAction();
    final NodeAction removeSubgroupsPopupAction = new RemoveSubgroupsAction();
    final NodeAction removeGroupKeepSubgroupsPopupAction = new RemoveGroupKeepSubgroupsAction();
    final NodeAction moveNodeUpPopupAction = new MoveNodeUpAction();
    final NodeAction moveNodeDownPopupAction = new MoveNodeDownAction();
    final NodeAction moveNodeLeftPopupAction = new MoveNodeLeftAction();
    final NodeAction moveNodeRightPopupAction = new MoveNodeRightAction();
    final NodeAction moveNodeUpAction = new MoveNodeUpAction();
    final NodeAction moveNodeDownAction = new MoveNodeDownAction();
    final NodeAction moveNodeLeftAction = new MoveNodeLeftAction();
    final NodeAction moveNodeRightAction = new MoveNodeRightAction();
    final NodeAction expandSubtreePopupAction = new ExpandSubtreeAction();
    final NodeAction collapseSubtreePopupAction = new CollapseSubtreeAction();
    final NodeAction sortDirectSubgroupsPopupAction = new SortDirectSubgroupsAction();
    final NodeAction sortAllSubgroupsPopupAction = new SortAllSubgroupsAction();
    final AddToGroupAction addToGroup = new AddToGroupAction(false);
    final AddToGroupAction moveToGroup = new AddToGroupAction(true);
    final RemoveFromGroupAction removeFromGroup = new RemoveFromGroupAction();

    private class EditGroupAction extends NodeAction {
        public EditGroupAction() {
            super(Globals.lang("Edit group"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            final AbstractGroup oldGroup = node.getGroup();
            final GroupDialog gd = new GroupDialog(frame, panel, oldGroup);
            gd.setVisible(true);
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                AbstractUndoableEdit undoAddPreviousEntries
                    = gd.getUndoForAddPreviousEntries();
                UndoableModifyGroup undo = new UndoableModifyGroup(
                        GroupSelector.this, groupsRoot, node, newGroup);
                node.setGroup(newGroup);
                revalidateGroups(node);
                // Store undo information.
                if (undoAddPreviousEntries == null) {
                    panel.undoManager.addEdit(undo);
                } else {
                    NamedCompound nc = new NamedCompound("Modify Group"); // JZTODO lyrics
                    nc.addEdit(undo);
                    nc.addEdit(undoAddPreviousEntries);
                    nc.end();
                    panel.undoManager.addEdit(nc);
                }
                panel.markBaseChanged();
                frame.output(Globals.lang("Modified group \"%0\".",
                        newGroup.getName()));
            }
        }
    }

    private class AddGroupAction extends NodeAction {
        public AddGroupAction() {
            super(Globals.lang("Add Group"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true);
            if (!gd.okPressed())
                return; // ignore
            final AbstractGroup newGroup = gd.getResultingGroup();
            final GroupTreeNode newNode = new GroupTreeNode(newGroup);
            if (node == null)
                groupsRoot.add(newNode);
            else
                ((GroupTreeNode) node.getParent()).insert(newNode, node
                        .getParent().getIndex(node) + 1);
            UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                    GroupSelector.this, groupsRoot, newNode,
                    UndoableAddOrRemoveGroup.ADD_NODE);
            revalidateGroups();
            groupsTree.expandPath(new TreePath(
                    (node != null ? node : groupsRoot).getPath()));
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Globals.lang("Added group \"%0\".",
                    newGroup.getName()));
        }
    }

    private class AddSubgroupAction extends NodeAction {
        public AddSubgroupAction() {
            super(Globals.lang("Add Subgroup"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true);
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
    }

    private class RemoveGroupAndSubgroupsAction extends NodeAction {
        public RemoveGroupAndSubgroupsAction() {
            super(Globals.lang("Remove group and subgroups"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
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
    }

    private class RemoveSubgroupsAction extends NodeAction {
        public RemoveSubgroupsAction() {
            super(Globals.lang("Remove all subgroups"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            final AbstractGroup group = node.getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("Remove all subgroups of \"%0\"?",group.getName()),
                    Globals.lang("Remove all subgroups"),
                    JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableModifySubtree undo = new UndoableModifySubtree(
                        GroupSelector.this, node,
                        "Remove all subgroups");
                node.removeAllChildren();
                revalidateGroups();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Globals.lang("Removed all subgroups of group \"%0\".",
                        group.getName()));
            }
        }
    }

    private class RemoveGroupKeepSubgroupsAction extends NodeAction {
        public RemoveGroupKeepSubgroupsAction() {
            super(Globals.lang("Remove group, keep subgroups"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
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
    }

    public TreePath getSelectionPath() {
        return groupsTree.getSelectionPath();
    }

    private class SortDirectSubgroupsAction extends NodeAction {
        public SortDirectSubgroupsAction() {
            super(Globals.lang("Immediate subgroups"));
        }
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNode node = getNodeToUse();
            final UndoableModifySubtree undo = new UndoableModifySubtree(
                    GroupSelector.this, node, Globals.lang("sort subgroups"));
            groupsTree.sort(node, false);
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Globals.lang("Sorted immediate subgroups."));
        }
    }

    private class SortAllSubgroupsAction extends NodeAction {
        public SortAllSubgroupsAction() {
            super(Globals.lang("All subgroups (recursively)"));
        }
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNode node = getNodeToUse();
            final UndoableModifySubtree undo = new UndoableModifySubtree(
                    GroupSelector.this, node, Globals.lang("sort subgroups"));
            groupsTree.sort(node, true);
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged(); // JZTODO lyrics
            frame.output(Globals.lang("Sorted all subgroups recursively."));
        }
    }

    public final AbstractAction clearHighlightAction = new AbstractAction(Globals.lang("Clear highlight")) {
        public void actionPerformed(ActionEvent ae) {
            groupsTree.setHighlight3Cells(null);
        }
    };

    private class ExpandSubtreeAction extends NodeAction {
        public ExpandSubtreeAction() {
            super(Globals.lang("Expand subtree"));
        }
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNode node = getNodeToUse();
            TreePath path = new TreePath(node.getPath());
            groupsTree.expandSubtree((GroupTreeNode) path.getLastPathComponent());
            revalidateGroups();
        }
    }

    private class CollapseSubtreeAction extends NodeAction {
        public CollapseSubtreeAction() {
            super(Globals.lang("Collapse subtree"));
        }
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNode node = getNodeToUse();
            TreePath path = new TreePath(node.getPath());
            groupsTree.collapseSubtree((GroupTreeNode) path.getLastPathComponent());
            revalidateGroups();
        }
    }

    private class MoveNodeUpAction extends NodeAction {
        public MoveNodeUpAction() {
            super(Globals.lang("Up"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            moveNodeUp(node, false);
        }
    }

    private class MoveNodeDownAction extends NodeAction {
        public MoveNodeDownAction() {
            super(Globals.lang("Down"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            moveNodeDown(node, false);
        }
    }

    private class MoveNodeLeftAction extends NodeAction {
        public MoveNodeLeftAction() {
            super(Globals.lang("Left"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            moveNodeLeft(node, false);
        }
    }

    private class MoveNodeRightAction extends NodeAction {
        public MoveNodeRightAction() {
            super(Globals.lang("Right"));
        }
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNode node = getNodeToUse();
            moveNodeRight(node, false);
        }
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeUp(GroupTreeNode node, boolean checkSingleSelection) {
        if (checkSingleSelection) {
            if (groupsTree.getSelectionCount() != 1) {
                frame.output(Globals.lang("Please select exactly one group to move."));
                return false; // not possible
            }
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveUp() || (undo = node.moveUp(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" up.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state (not really needed when
        // moving among siblings, but I'm paranoid)
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(undo, node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeDown(GroupTreeNode node, boolean checkSingleSelection) {
        if (checkSingleSelection) {
            if (groupsTree.getSelectionCount() != 1) {
                frame.output(Globals.lang("Please select exactly one group to move."));
                return false; // not possible
            }
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveDown() || (undo = node.moveDown(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" down.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state (not really needed when
        // moving among siblings, but I'm paranoid)
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(undo, node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeLeft(GroupTreeNode node, boolean checkSingleSelection) {
        if (checkSingleSelection) {
            if (groupsTree.getSelectionCount() != 1) {
                frame.output(Globals.lang("Please select exactly one group to move."));
                return false; // not possible
            }
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveLeft() || (undo = node.moveLeft(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" left.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(undo, node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeRight(GroupTreeNode node, boolean checkSingleSelection) {
        if (checkSingleSelection) {
            if (groupsTree.getSelectionCount() != 1) {
                frame.output(Globals.lang("Please select exactly one group to move."));
                return false; // not possible
            }
        }
        AbstractUndoableEdit undo = null;
        if (!node.canMoveRight() || (undo = node.moveRight(GroupSelector.this)) == null) {
            frame.output(Globals.lang(
                    "Cannot move group \"%0\" right.", node.getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
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
        panel.updateEntryEditorIfShowing();
        final String groupName = node.getGroup().getName();
        if (assignedEntries == 1)
            frame.output(Globals.lang("Assigned 1 entry to group \"%0\".", groupName));
        else
            frame.output(Globals.lang("Assigned %0 entries to group \"%1\".",
                    String.valueOf(assignedEntries), groupName));
    }

    JMenu moveSubmenu = new JMenu(Globals.lang("Move"));
    JMenu sortSubmenu = new JMenu(Globals.lang("Sort alphabetically")); // JZTODO lyrics

    public GroupTreeNode getGroupTreeRoot() {
        return groupsRoot;
    }

    public Enumeration<TreePath> getExpandedPaths() {
        return groupsTree.getExpandedDescendants(
                new TreePath(groupsRoot.getPath()));
    }


    /** panel may be null to indicate that no file is currently open. */
    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel == null) { // hide groups
            frame.sidePaneManager.hide("groups");
            return;
        }
        MetaData metaData = panel.metaData();
        if (metaData.getGroups() != null) {
            setGroups(metaData.getGroups());
        } else {
            GroupTreeNode newGroupsRoot = new GroupTreeNode(new AllEntriesGroup());
            metaData.setGroups(newGroupsRoot);
            setGroups(newGroupsRoot);
        }

        // auto show/hide groups interface
        if (Globals.prefs.getBoolean("groupAutoShow") &&
                        !groupsRoot.isLeaf()) { // groups were defined
            frame.sidePaneManager.show("groups");
            frame.groupToggle.setSelected(true);
        } else if (Globals.prefs.getBoolean("groupAutoHide") &&
                        groupsRoot.isLeaf()) { // groups were not defined
            frame.sidePaneManager.hide("groups");
            frame.groupToggle.setSelected(false);
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

    /**
     * Highlight all groups that contain any/all of the specified entries.
     * If entries is null or has zero length, highlight is cleared.
     */
    public void showMatchingGroups(BibtexEntry[] entries, boolean requireAll) {
        if (entries == null || entries.length == 0) { // nothing selected
            groupsTree.setHighlight3Cells(null);
            groupsTree.revalidate();
            return;
        }
        GroupTreeNode node;
        AbstractGroup group;
        Vector<GroupTreeNode> vec = new Vector<GroupTreeNode>();
        for (Enumeration<GroupTreeNode> e = groupsRoot.preorderEnumeration(); e.hasMoreElements(); ) {
            node = e.nextElement();
            group = node.getGroup();
            int i;
            for (i = 0; i < entries.length; ++i) {
                if (requireAll) {
                    if (!group.contains(entries[i]))
                        break;
                } else {
                    if (group.contains(entries[i]))
                        vec.add(node);
                }
            }
            if (requireAll && i >= entries.length) // did not break from loop
                vec.add(node);
        }
        groupsTree.setHighlight3Cells(vec.toArray());
        // ensure that all highlighted nodes are visible
        for (int i = 0; i < vec.size(); ++i) {
            node = (GroupTreeNode)vec.elementAt(i).getParent();
            if (node != null)
                groupsTree.expandPath(new TreePath(node.getPath()));
        }
        groupsTree.revalidate();
    }

    /** Show groups that, if selected, would show at least one
     * of the entries found in the specified search. */
    protected void showOverlappingGroups(List<BibtexEntry> matches) { //DatabaseSearch search) {
      GroupTreeNode node;
      SearchRule rule;
      BibtexEntry entry;
      Vector<GroupTreeNode> vec = new Vector<GroupTreeNode>();
      Map<String, String> dummyMap = new HashMap<String, String>(); // just because I don't want to use null...
      for (Enumeration<GroupTreeNode> e = groupsRoot.depthFirstEnumeration(); e.hasMoreElements(); ) {
          node = e.nextElement();
          rule = node.getSearchRule();
          for (Iterator<BibtexEntry> it = matches.iterator(); it.hasNext(); ) {
              entry = it.next();
              if (rule.applyRule(dummyMap, entry) == 0)
                      continue;
              vec.add(node);
              break;
          }
      }
      groupsTree.setHighlight2Cells(vec.toArray());
    }
}

