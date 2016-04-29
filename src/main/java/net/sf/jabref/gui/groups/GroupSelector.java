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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneComponent;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.groups.AbstractGroup;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.groups.MoveGroupChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchMatcher;
import net.sf.jabref.logic.search.matchers.MatcherSet;
import net.sf.jabref.logic.search.matchers.MatcherSets;
import net.sf.jabref.logic.search.matchers.NotMatcher;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The whole UI component holding the groups tree and the buttons
 */
public class GroupSelector extends SidePaneComponent implements TreeSelectionListener {

    private static final Log LOGGER = LogFactory.getLog(GroupSelector.class);

    private final JButton newButton = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
    private final JButton refresh = new JButton(IconTheme.JabRefIcon.REFRESH.getSmallIcon());
    private final JButton autoGroup = new JButton(IconTheme.JabRefIcon.AUTO_GROUP.getSmallIcon());
    private final JButton openset = new JButton(Localization.lang("Settings"));
    private final GroupsTree groupsTree;
    private DefaultTreeModel groupsTreeModel;
    private GroupTreeNodeViewModel groupsRoot;
    protected final JabRefFrame frame;
    private final JPopupMenu groupsContextMenu = new JPopupMenu();
    private final JPopupMenu settings = new JPopupMenu();
    private final JRadioButtonMenuItem hideNonHits;
    private final JRadioButtonMenuItem grayOut;
    private final JRadioButtonMenuItem andCb = new JRadioButtonMenuItem(Localization.lang("Intersection"), true);
    private final JRadioButtonMenuItem floatCb = new JRadioButtonMenuItem(Localization.lang("Float"), true);
    private final JCheckBoxMenuItem invCb = new JCheckBoxMenuItem(Localization.lang("Inverted"), false);
    private final JCheckBoxMenuItem select = new JCheckBoxMenuItem(Localization.lang("Select matches"), false);
    private final JCheckBoxMenuItem showOverlappingGroups = new JCheckBoxMenuItem(
            Localization.lang("Highlight overlapping groups"));
    private final JCheckBoxMenuItem showNumberOfElements = new JCheckBoxMenuItem(
            Localization.lang("Show number of elements contained in each group"));
    private final JCheckBoxMenuItem autoAssignGroup = new JCheckBoxMenuItem(
            Localization.lang("Automatically assign new entry to selected groups"));
    private final JCheckBoxMenuItem editModeCb = new JCheckBoxMenuItem(Localization.lang("Edit group membership"),
            false);
    private final Border editModeBorder = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(2, 2, 2, 2, Color.RED), "Edit mode", TitledBorder.RIGHT, TitledBorder.TOP,
            Font.getFont("Default"), Color.RED);
    private boolean editModeIndicator;

    private static final String MOVE_ONE_GROUP = Localization.lang("Please select exactly one group to move.");

    private final JMenu moveSubmenu = new JMenu(Localization.lang("Move"));
    private final JMenu sortSubmenu = new JMenu(Localization.lang("Sort alphabetically"));

    private final AbstractAction editGroupAction = new EditGroupAction();
    private final NodeAction editGroupPopupAction = new EditGroupAction();
    private final NodeAction addGroupPopupAction = new AddGroupAction();
    private final NodeAction addSubgroupPopupAction = new AddSubgroupAction();
    private final NodeAction removeGroupAndSubgroupsPopupAction = new RemoveGroupAndSubgroupsAction();
    private final NodeAction removeSubgroupsPopupAction = new RemoveSubgroupsAction();
    private final NodeAction removeGroupKeepSubgroupsPopupAction = new RemoveGroupKeepSubgroupsAction();
    private final NodeAction moveNodeUpPopupAction = new MoveNodeUpAction();
    private final NodeAction moveNodeDownPopupAction = new MoveNodeDownAction();
    private final NodeAction moveNodeLeftPopupAction = new MoveNodeLeftAction();
    private final NodeAction moveNodeRightPopupAction = new MoveNodeRightAction();
    private final NodeAction expandSubtreePopupAction = new ExpandSubtreeAction();
    private final NodeAction collapseSubtreePopupAction = new CollapseSubtreeAction();
    private final NodeAction sortDirectSubgroupsPopupAction = new SortDirectSubgroupsAction();
    private final NodeAction sortAllSubgroupsPopupAction = new SortAllSubgroupsAction();
    private final AddToGroupAction addToGroup = new AddToGroupAction(false);
    private final AddToGroupAction moveToGroup = new AddToGroupAction(true);
    private final RemoveFromGroupAction removeFromGroup = new RemoveFromGroupAction();


    /**
     * The first element for each group defines which field to use for the quicksearch. The next two define the name and
     * regexp for the group.
     */
    public GroupSelector(JabRefFrame frame, SidePaneManager manager) {
        super(manager, IconTheme.JabRefIcon.TOGGLE_GROUPS.getIcon(), Localization.lang("Groups"));

        this.frame = frame;
        hideNonHits = new JRadioButtonMenuItem(Localization.lang("Hide non-hits"),
                !Globals.prefs.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS));
        grayOut = new JRadioButtonMenuItem(Localization.lang("Gray out non-hits"),
                Globals.prefs.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS));
        ButtonGroup nonHits = new ButtonGroup();
        nonHits.add(hideNonHits);
        nonHits.add(grayOut);
        floatCb.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_FLOAT_SELECTIONS, floatCb.isSelected());
            }
        });
        andCb.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS, andCb.isSelected());
            }
        });
        invCb.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_INVERT_SELECTIONS, invCb.isSelected());
            }
        });
        showOverlappingGroups.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_SHOW_OVERLAPPING, showOverlappingGroups.isSelected());
                if (!showOverlappingGroups.isSelected()) {
                    groupsTree.setHighlight2Cells(null);
                }
            }
        });

        select.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_SELECT_MATCHES, select.isSelected());
            }
        });
        grayOut.addChangeListener(event -> Globals.prefs.putBoolean(JabRefPreferences.GRAY_OUT_NON_HITS, grayOut.isSelected()));

        JRadioButtonMenuItem highlCb = new JRadioButtonMenuItem(Localization.lang("Highlight"), false);
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_FLOAT_SELECTIONS)) {

            floatCb.setSelected(true);
            highlCb.setSelected(false);
        } else {
            highlCb.setSelected(true);
            floatCb.setSelected(false);
        }
        JRadioButtonMenuItem orCb = new JRadioButtonMenuItem(Localization.lang("Union"), false);
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS)) {
            andCb.setSelected(true);
            orCb.setSelected(false);
        } else {
            orCb.setSelected(true);
            andCb.setSelected(false);
        }

        showNumberOfElements.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Globals.prefs.putBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS,
                        showNumberOfElements.isSelected());
                if (groupsTree != null) {
                    groupsTree.invalidate();
                    groupsTree.repaint();
                }
            }
        });

        autoAssignGroup.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                Globals.prefs.putBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP, autoAssignGroup.isSelected());
            }
        });

        invCb.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GROUP_INVERT_SELECTIONS));
        showOverlappingGroups.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_OVERLAPPING));
        select.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GROUP_SELECT_MATCHES));
        editModeIndicator = Globals.prefs.getBoolean(JabRefPreferences.EDIT_GROUP_MEMBERSHIP_MODE);
        editModeCb.setSelected(editModeIndicator);
        showNumberOfElements.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS));
        autoAssignGroup.setSelected(Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP));

        openset.setMargin(new Insets(0, 0, 0, 0));
        settings.add(andCb);
        settings.add(orCb);
        settings.addSeparator();
        settings.add(invCb);
        settings.addSeparator();
        settings.add(select);
        settings.addSeparator();
        settings.add(editModeCb);
        settings.addSeparator();
        settings.add(grayOut);
        settings.add(hideNonHits);
        settings.addSeparator();
        settings.add(showOverlappingGroups);
        settings.addSeparator();
        settings.add(showNumberOfElements);
        settings.add(autoAssignGroup);
        // settings.add(moreRow);
        // settings.add(lessRow);
        openset.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!settings.isVisible()) {
                    JButton src = (JButton) e.getSource();
                    showNumberOfElements
                            .setSelected(Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS));
                    autoAssignGroup.setSelected(Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP));
                    settings.show(src, 0, openset.getHeight());
                }
            }
        });

        editModeCb.addActionListener(e -> setEditMode(editModeCb.getState()));

        int butSize = newButton.getIcon().getIconHeight() + 5;
        Dimension butDim = new Dimension(butSize, butSize);
        //Dimension butDimSmall = new Dimension(20, 20);

        newButton.setPreferredSize(butDim);
        newButton.setMinimumSize(butDim);
        refresh.setPreferredSize(butDim);
        refresh.setMinimumSize(butDim);
        JButton helpButton = new HelpAction(Localization.lang("Help on groups"), HelpFiles.GROUP)
                .getHelpButton();
        helpButton.setPreferredSize(butDim);
        helpButton.setMinimumSize(butDim);
        autoGroup.setPreferredSize(butDim);
        autoGroup.setMinimumSize(butDim);
        openset.setPreferredSize(butDim);
        openset.setMinimumSize(butDim);
        Insets butIns = new Insets(0, 0, 0, 0);
        helpButton.setMargin(butIns);
        openset.setMargin(butIns);
        newButton.addActionListener(e -> {
            GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true);
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                groupsRoot.addNewGroup(newGroup, panel.undoManager);
                panel.markBaseChanged();
                frame.output(Localization.lang("Created group \"%0\".", newGroup.getName()));
            }
        });
        refresh.addActionListener(e -> revalidateGroups());
        andCb.addActionListener(e -> valueChanged(null));
        orCb.addActionListener(e -> valueChanged(null));
        invCb.addActionListener(e -> valueChanged(null));
        showOverlappingGroups.addActionListener(e -> valueChanged(null));
        autoGroup.addActionListener(e -> {
            AutoGroupDialog gd = new AutoGroupDialog(frame, panel, groupsRoot,
                    Globals.prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD), " .,", ",");
            gd.setVisible(true);
            // gd does the operation itself
        });
        floatCb.addActionListener(e -> valueChanged(null));
        highlCb.addActionListener(e -> valueChanged(null));
        select.addActionListener(e -> valueChanged(null));
        hideNonHits.addActionListener(e -> valueChanged(null));
        grayOut.addActionListener(e -> valueChanged(null));
        newButton.setToolTipText(Localization.lang("New group"));
        refresh.setToolTipText(Localization.lang("Refresh view"));
        andCb.setToolTipText(Localization.lang("Display only entries belonging to all selected groups."));
        orCb.setToolTipText(Localization.lang("Display all entries belonging to one or more of the selected groups."));
        autoGroup.setToolTipText(Localization.lang("Automatically create groups for database."));
        invCb.setToolTipText("<html>" + Localization.lang("Show entries <b>not</b> in group selection") + "</html>");
        showOverlappingGroups.setToolTipText(
                Localization.lang("Highlight groups that contain entries contained in any currently selected group"));
        floatCb.setToolTipText(Localization.lang("Move entries in group selection to the top"));
        highlCb.setToolTipText(Localization.lang("Gray out entries not in group selection"));
        select.setToolTipText(Localization.lang("Select entries in group selection"));
        editModeCb.setToolTipText(Localization.lang("Click group to toggle membership of selected entries"));
        ButtonGroup bgr = new ButtonGroup();
        bgr.add(andCb);
        bgr.add(orCb);
        ButtonGroup visMode = new ButtonGroup();
        visMode.add(floatCb);
        visMode.add(highlCb);

        JPanel main = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        main.setLayout(gbl);

        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        //con.insets = new Insets(0, 0, 2, 0);
        con.weightx = 1;
        con.gridwidth = 1;
        con.gridx = 0;
        con.gridy = 0;
        //con.insets = new Insets(1, 1, 1, 1);
        gbl.setConstraints(newButton, con);
        main.add(newButton);
        con.gridx = 1;
        gbl.setConstraints(refresh, con);
        main.add(refresh);
        con.gridx = 2;
        gbl.setConstraints(autoGroup, con);
        main.add(autoGroup);
        con.gridx = 3;
        con.gridwidth = GridBagConstraints.REMAINDER;

        gbl.setConstraints(helpButton, con);
        main.add(helpButton);

        // header.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        // helpButton.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        groupsTree = new GroupsTree(this);
        groupsTree.addTreeSelectionListener(this);

        JScrollPane sp = new JScrollPane(groupsTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weighty = 1;
        con.gridx = 0;
        con.gridwidth = 4;
        con.gridy = 1;
        gbl.setConstraints(sp, con);
        main.add(sp);

        JPanel pan = new JPanel();
        GridBagLayout gb = new GridBagLayout();
        con.weighty = 0;
        gbl.setConstraints(pan, con);
        pan.setLayout(gb);
        con.insets = new Insets(0, 0, 0, 0);
        con.gridx = 0;
        con.gridy = 0;
        con.weightx = 1;
        con.gridwidth = 4;
        con.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(openset, con);
        pan.add(openset);

        con.gridwidth = 6;
        con.gridy = 1;
        con.gridx = 0;
        con.fill = GridBagConstraints.HORIZONTAL;

        con.gridy = 2;
        con.gridx = 0;
        con.gridwidth = 4;
        gbl.setConstraints(pan, con);
        main.add(pan);
        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        add(main, BorderLayout.CENTER);
        setEditMode(editModeIndicator);
        definePopup();
        NodeAction moveNodeUpAction = new MoveNodeUpAction();
        moveNodeUpAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
        NodeAction moveNodeDownAction = new MoveNodeDownAction();
        moveNodeDownAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
        NodeAction moveNodeLeftAction = new MoveNodeLeftAction();
        moveNodeLeftAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
        NodeAction moveNodeRightAction = new MoveNodeRightAction();
        moveNodeRightAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));


        setGroups(new GroupTreeNode(new AllEntriesGroup()));
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

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e.getPoint().y);
                if (path == null) {
                    return;
                }
                GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
                // the root node is "AllEntries" and cannot be edited
                if (node.getNode().isRoot()) {
                    return;
                }
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) { // edit
                    editGroupAction.actionPerformed(null); // dummy event
                } else if ((e.getClickCount() == 1) && (e.getButton() == MouseEvent.BUTTON1)) {
                    annotationEvent(node);
                }
            }
        });
        // be sure to remove a possible border highlight when the popup menu
        // disappears
        groupsContextMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // nothing to do
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                groupsTree.setHighlightBorderCell(null);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                groupsTree.setHighlightBorderCell(null);
            }
        });
    }

    private void showPopup(MouseEvent e) {
        final TreePath path = groupsTree.getPathForLocation(e.getPoint().x, e.getPoint().y);
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
            GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
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
            if (node.canBeEdited()) {
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
            expandSubtreePopupAction
                    .setEnabled(groupsTree.isCollapsed(path) || groupsTree.hasCollapsedDescendant(path));
            collapseSubtreePopupAction
                    .setEnabled(groupsTree.isExpanded(path) || groupsTree.hasExpandedDescendant(path));
            sortSubmenu.setEnabled(!node.isLeaf());
            removeSubgroupsPopupAction.setEnabled(!node.isLeaf());
            moveNodeUpPopupAction.setEnabled(node.canMoveUp());
            moveNodeDownPopupAction.setEnabled(node.canMoveDown());
            moveNodeLeftPopupAction.setEnabled(node.canMoveLeft());
            moveNodeRightPopupAction.setEnabled(node.canMoveRight());
            moveSubmenu.setEnabled(moveNodeUpPopupAction.isEnabled() || moveNodeDownPopupAction.isEnabled()
                    || moveNodeLeftPopupAction.isEnabled() || moveNodeRightPopupAction.isEnabled());
            moveNodeUpPopupAction.setNode(node);
            moveNodeDownPopupAction.setNode(node);
            moveNodeLeftPopupAction.setNode(node);
            moveNodeRightPopupAction.setNode(node);
            // add/remove entries to/from group
            List<BibEntry> selection = frame.getCurrentBasePanel().getSelectedEntries();
            if (selection.size() > 0) {
                if (node.canAddEntries(selection)) {
                    addToGroup.setNode(node);
                    addToGroup.setBasePanel(panel);
                    addToGroup.setEnabled(true);
                    moveToGroup.setNode(node);
                    moveToGroup.setBasePanel(panel);
                    moveToGroup.setEnabled(true);
                }
                if (node.canRemoveEntries(selection)) {
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

    private void setEditMode(boolean editMode) {
        Globals.prefs.putBoolean(JabRefPreferences.EDIT_GROUP_MEMBERSHIP_MODE, editModeIndicator);
        editModeIndicator = editMode;

        if (editMode) {
            groupsTree.setBorder(editModeBorder);
            this.setTitle("<html><font color='red'>Groups Edit mode</font></html>");
        } else {
            groupsTree.setBorder(null);
            this.setTitle(Localization.lang("Groups"));
        }
        groupsTree.revalidate();
        groupsTree.repaint();
    }

    private void annotationEvent(GroupTreeNodeViewModel node) {
        if (editModeIndicator) {
            LOGGER.info("Performing annotation " + node.getName());
            List<BibEntry> entries = panel.getSelectedEntries();
            node.changeEntriesTo(entries, panel.undoManager);
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
            updateShownEntriesAccordingToSelectedGroups();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (panel == null) {
            return; // ignore this event (happens for example if the file was closed)
        }
        if (getLeafsOfSelection().stream().allMatch(node -> node.isAllEntriesGroup())) {
            panel.mainTable.getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.DISABLED);
            if (showOverlappingGroups.isSelected()) {
                groupsTree.setHighlight2Cells(null);
            }
            frame.output(Localization.lang("Displaying no groups") + ".");
            return;
        }

        if (!editModeIndicator) {
            updateShownEntriesAccordingToSelectedGroups();
        }
    }

    private void updateShownEntriesAccordingToSelectedGroups() {
        final MatcherSet searchRules = MatcherSets
                .build(andCb.isSelected() ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);

        for(GroupTreeNodeViewModel node : getLeafsOfSelection()) {
            SearchMatcher searchRule = node.getNode().getSearchRule();
            searchRules.addRule(searchRule);
        }
        SearchMatcher searchRule = invCb.isSelected() ? new NotMatcher(searchRules) : searchRules;
        GroupingWorker worker = new GroupingWorker(searchRule);
        worker.getWorker().run();
        worker.getCallBack().update();
    }

    private List<GroupTreeNodeViewModel> getLeafsOfSelection() {
        TreePath[] selection = groupsTree.getSelectionPaths();
        if((selection == null) || (selection.length == 0)) {
            return new ArrayList<>();
        }

        List<GroupTreeNodeViewModel> selectedLeafs = new ArrayList<>(selection.length);
        for (TreePath path : selection) {
            selectedLeafs.add((GroupTreeNodeViewModel) path.getLastPathComponent());
        }
        return selectedLeafs;
    }

    private GroupTreeNodeViewModel getFirstSelectedNode() {
        TreePath path = groupsTree.getSelectionPath();
        if (path != null) {
            return (GroupTreeNodeViewModel) path.getLastPathComponent();
        }
        return null;
    }

    class GroupingWorker extends AbstractWorker {

        private final SearchMatcher matcher;
        private final List<BibEntry> matches = new ArrayList<>();
        private final boolean showOverlappingGroupsP;

        public GroupingWorker(SearchMatcher matcher) {
            this.matcher = matcher;
            showOverlappingGroupsP = showOverlappingGroups.isSelected();
        }

        @Override
        public void run() {
            for (BibEntry entry : panel.getDatabase().getEntries()) {
                boolean hit = matcher.isMatch(entry);
                entry.setGroupHit(hit);
                if (hit && showOverlappingGroupsP) {
                    matches.add(entry);
                }
            }
        }

        @Override
        public void update() {
            // Show the result in the chosen way:
            if (hideNonHits.isSelected()) {
                panel.mainTable.getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.FILTER);
            } else if (grayOut.isSelected()) {
                panel.mainTable.getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.FLOAT);
            }
            panel.mainTable.getTableModel().updateSortOrder();
            panel.mainTable.getTableModel().updateGroupFilter();
            panel.mainTable.scrollTo(0);

            if (showOverlappingGroupsP) {
                showOverlappingGroups(matches);
            }
            frame.output(Localization.lang("Updated group selection") + ".");
        }
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has been changed) and maintain the current
     * selection and expansion state.
     */
    public void revalidateGroups() {
        revalidateGroups(null);
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has been changed) and maintain the current
     * selection and expansion state.
     *
     * @param node If this is non-null, the view is scrolled to make it visible.
     */
    private void revalidateGroups(GroupTreeNodeViewModel node) {
        revalidateGroups(groupsTree.getSelectionPaths(), getExpandedPaths(), node);
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has been changed) and set the specified
     * selection and expansion state.
     */
    public void revalidateGroups(TreePath[] selectionPaths, Enumeration<TreePath> expandedNodes) {
        revalidateGroups(selectionPaths, expandedNodes, null);
    }

    /**
     * Revalidate the groups tree (e.g. after the data stored in the model has been changed) and set the specified
     * selection and expansion state.
     *
     * @param node If this is non-null, the view is scrolled to make it visible.
     */
    private void revalidateGroups(TreePath[] selectionPaths, Enumeration<TreePath> expandedNodes,
            GroupTreeNodeViewModel node) {
        groupsTreeModel.reload();
        groupsTree.clearSelection();
        if (selectionPaths != null) {
            groupsTree.setSelectionPaths(selectionPaths);
        }
        // tree is completely collapsed here
        if (expandedNodes != null) {
            while (expandedNodes.hasMoreElements()) {
                groupsTree.expandPath(expandedNodes.nextElement());
            }
        }
        groupsTree.revalidate();
        if (node != null) {
            groupsTree.scrollPathToVisible(node.getTreePath());
        }
    }

    @Override
    public void componentOpening() {
        valueChanged(null);
    }

    @Override
    public int getRescalingWeight() {
        return 1;
    }

    @Override
    public void componentClosing() {
        if (panel != null) {// panel may be null if no file is open any more
            panel.mainTable.getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.DISABLED);
        }
        frame.groupToggle.setSelected(false);
    }

    private void setGroups(GroupTreeNode groupsRoot) {
        this.groupsRoot = new GroupTreeNodeViewModel(groupsRoot);
        this.groupsRoot.subscribeToDescendantChanged(source -> groupsTreeModel.nodeStructureChanged(source));
        groupsTreeModel = new DefaultTreeModel(this.groupsRoot);
        groupsTree.setModel(groupsTreeModel);
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_EXPAND_TREE)) {
            this.groupsRoot.expandSubtree(groupsTree);
        }
    }

    /**
     * Adds the specified node as a child of the current root. The group contained in <b>newGroups </b> must not be of
     * type AllEntriesGroup, since every tree has exactly one AllEntriesGroup (its root). The <b>newGroups </b> are
     * inserted directly, i.e. they are not deepCopy()'d.
     */
    public void addGroups(GroupTreeNode newGroups, CompoundEdit ce) {
        // TODO: This shouldn't be a method of GroupSelector

        // paranoia: ensure that there are never two instances of AllEntriesGroup
        if (newGroups.getGroup() instanceof AllEntriesGroup) {
            return; // this should be impossible anyway
        }
        newGroups.moveTo(groupsRoot.getNode());
        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot,
                new GroupTreeNodeViewModel(newGroups), UndoableAddOrRemoveGroup.ADD_NODE);
        ce.addEdit(undo);
    }

    private abstract class NodeAction extends AbstractAction {

        private GroupTreeNodeViewModel node;

        public NodeAction(String s) {
            super(s);
        }

        public void setNode(GroupTreeNodeViewModel node) {
            this.node = node;
        }

        /**
         * Returns the node to use in this action. If a node has been set explicitly (via setNode), it is returned.
         * Otherwise, the first node in the current selection is returned. If all this fails, null is returned.
         */
        public GroupTreeNodeViewModel getNodeToUse() {
            if (node != null) {
                return node;
            }
            return getFirstSelectedNode();
        }
    }

    private class EditGroupAction extends NodeAction {

        public EditGroupAction() {
            super(Localization.lang("Edit group"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            final AbstractGroup oldGroup = node.getNode().getGroup();
            final GroupDialog gd = new GroupDialog(frame, panel, oldGroup);
            gd.setVisible(true);
            if (gd.okPressed()) {
                AbstractGroup newGroup = gd.getResultingGroup();
                AbstractUndoableEdit undoAddPreviousEntries = gd.getUndoForAddPreviousEntries();
                UndoableModifyGroup undo = new UndoableModifyGroup(GroupSelector.this, groupsRoot, node, newGroup);
                node.getNode().setGroup(newGroup);
                revalidateGroups(node);
                // Store undo information.
                if (undoAddPreviousEntries == null) {
                    panel.undoManager.addEdit(undo);
                } else {
                    NamedCompound nc = new NamedCompound("Modify Group");
                    nc.addEdit(undo);
                    nc.addEdit(undoAddPreviousEntries);
                    nc.end();
                    panel.undoManager.addEdit(nc);
                }
                panel.markBaseChanged();
                frame.output(Localization.lang("Modified group \"%0\".", newGroup.getName()));
            }
        }
    }

    private class AddGroupAction extends NodeAction {

        public AddGroupAction() {
            super(Localization.lang("Add group"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true);
            if (!gd.okPressed()) {
                return; // ignore
            }
            final AbstractGroup newGroup = gd.getResultingGroup();
            final GroupTreeNode newNode = new GroupTreeNode(newGroup);
            final GroupTreeNodeViewModel node = getNodeToUse();
            if (node == null) {
                groupsRoot.getNode().addChild(newNode);
            } else {
                ((GroupTreeNodeViewModel)node.getParent()).getNode().addChild(newNode, node.getNode().getPositionInParent() + 1);
            }
            UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot,
                    new GroupTreeNodeViewModel(newNode), UndoableAddOrRemoveGroup.ADD_NODE);
            groupsTree.expandPath((node == null ? groupsRoot : node).getTreePath());
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Localization.lang("Added group \"%0\".", newGroup.getName()));
        }
    }

    private class AddSubgroupAction extends NodeAction {

        public AddSubgroupAction() {
            super(Localization.lang("Add subgroup"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupDialog gd = new GroupDialog(frame, panel, null);
            gd.setVisible(true);
            if (!gd.okPressed()) {
                return; // ignore
            }
            final AbstractGroup newGroup = gd.getResultingGroup();
            final GroupTreeNode newNode = new GroupTreeNode(newGroup);
            final GroupTreeNodeViewModel node = getNodeToUse();
            node.getNode().addChild(newNode);
            UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot,
                    new GroupTreeNodeViewModel(newNode), UndoableAddOrRemoveGroup.ADD_NODE);
            groupsTree.expandPath(node.getTreePath());
            // Store undo information.
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Localization.lang("Added group \"%0\".", newGroup.getName()));
        }
    }

    private class RemoveGroupAndSubgroupsAction extends NodeAction {

        public RemoveGroupAndSubgroupsAction() {
            super(Localization.lang("Remove group and subgroups"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            final AbstractGroup group = node.getNode().getGroup();
            int conf = JOptionPane.showConfirmDialog(frame,
                    Localization.lang("Remove group \"%0\" and its subgroups?", group.getName()),
                    Localization.lang("Remove group and subgroups"), JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node,
                        UndoableAddOrRemoveGroup.REMOVE_NODE_AND_CHILDREN);
                node.getNode().removeFromParent();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Localization.lang("Removed group \"%0\" and its subgroups.", group.getName()));
            }
        }
    }

    private class RemoveSubgroupsAction extends NodeAction {

        public RemoveSubgroupsAction() {
            super(Localization.lang("Remove subgroups"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            int conf = JOptionPane.showConfirmDialog(frame,
                    Localization.lang("Remove all subgroups of \"%0\"?", node.getName()),
                    Localization.lang("Remove subgroups"), JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableModifySubtree undo = new UndoableModifySubtree(getGroupTreeRoot(),
                        node, "Remove subgroups");
                node.getNode().removeAllChildren();
                //revalidateGroups();
                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Localization.lang("Removed all subgroups of group \"%0\".", node.getName()));
            }
        }
    }

    private class RemoveGroupKeepSubgroupsAction extends NodeAction {

        public RemoveGroupKeepSubgroupsAction() {
            super(Localization.lang("Remove group, keep subgroups"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            final AbstractGroup group = node.getNode().getGroup();
            int conf = JOptionPane.showConfirmDialog(frame, Localization.lang("Remove group \"%0\"?", group.getName()),
                    Localization.lang("Remove group"), JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node,
                        UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN);
                final GroupTreeNodeViewModel parent = (GroupTreeNodeViewModel)node.getParent();
                node.getNode().removeFromParent();
                node.getNode().moveAllChildrenTo(parent.getNode(), parent.getIndex(node));

                // Store undo information.
                panel.undoManager.addEdit(undo);
                panel.markBaseChanged();
                frame.output(Localization.lang("Removed group \"%0\".", group.getName()));
            }
        }
    }


    public TreePath getSelectionPath() {
        return groupsTree.getSelectionPath();
    }


    private class SortDirectSubgroupsAction extends NodeAction {

        public SortDirectSubgroupsAction() {
            super(Localization.lang("Immediate subgroups"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            final UndoableModifySubtree undo = new UndoableModifySubtree(getGroupTreeRoot(), node,
                    Localization.lang("sort subgroups"));
            groupsTree.sort(node, false);
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Localization.lang("Sorted immediate subgroups."));
        }
    }

    private class SortAllSubgroupsAction extends NodeAction {

        public SortAllSubgroupsAction() {
            super(Localization.lang("All subgroups (recursively)"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            final UndoableModifySubtree undo = new UndoableModifySubtree(getGroupTreeRoot(), node,
                    Localization.lang("sort subgroups"));
            groupsTree.sort(node, true);
            panel.undoManager.addEdit(undo);
            panel.markBaseChanged();
            frame.output(Localization.lang("Sorted all subgroups recursively."));
        }
    }

    private class ExpandSubtreeAction extends NodeAction {

        public ExpandSubtreeAction() {
            super(Localization.lang("Expand subtree"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            getNodeToUse().expandSubtree(groupsTree);
            revalidateGroups();
        }
    }

    private class CollapseSubtreeAction extends NodeAction {

        public CollapseSubtreeAction() {
            super(Localization.lang("Collapse subtree"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            getNodeToUse().collapseSubtree(groupsTree);
            revalidateGroups();
        }
    }

    private class MoveNodeUpAction extends NodeAction {

        public MoveNodeUpAction() {
            super(Localization.lang("Up"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            moveNodeUp(node, false);
        }
    }

    private class MoveNodeDownAction extends NodeAction {

        public MoveNodeDownAction() {
            super(Localization.lang("Down"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            moveNodeDown(node, false);
        }
    }

    private class MoveNodeLeftAction extends NodeAction {

        public MoveNodeLeftAction() {
            super(Localization.lang("Left"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            moveNodeLeft(node, false);
        }
    }

    private class MoveNodeRightAction extends NodeAction {

        public MoveNodeRightAction() {
            super(Localization.lang("Right"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GroupTreeNodeViewModel node = getNodeToUse();
            moveNodeRight(node, false);
        }
    }


    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeUp(GroupTreeNodeViewModel node, boolean checkSingleSelection) {
        if (checkSingleSelection && (groupsTree.getSelectionCount() != 1)) {
            frame.output(MOVE_ONE_GROUP);
            return false; // not possible
        }
        Optional<MoveGroupChange> moveChange;
        if (!node.canMoveUp() || (! (moveChange = node.moveUp()).isPresent())) {
            frame.output(Localization.lang("Cannot move group \"%0\" up.", node.getNode().getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state (not really needed when
        // moving among siblings, but I'm paranoid)
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(moveChange.get(), node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeDown(GroupTreeNodeViewModel node, boolean checkSingleSelection) {
        if (checkSingleSelection && (groupsTree.getSelectionCount() != 1)) {
            frame.output(MOVE_ONE_GROUP);
            return false; // not possible
        }
        Optional<MoveGroupChange> moveChange;
        if (!node.canMoveDown() || (! (moveChange = node.moveDown()).isPresent())) {
            frame.output(Localization.lang("Cannot move group \"%0\" down.", node.getNode().getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state (not really needed when
        // moving among siblings, but I'm paranoid)
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(moveChange.get(), node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeLeft(GroupTreeNodeViewModel node, boolean checkSingleSelection) {
        if (checkSingleSelection && (groupsTree.getSelectionCount() != 1)) {
            frame.output(MOVE_ONE_GROUP);
            return false; // not possible
        }
        Optional<MoveGroupChange> moveChange;
        if (!node.canMoveLeft() || (! (moveChange = node.moveLeft()).isPresent())) {
            frame.output(Localization.lang("Cannot move group \"%0\" left.", node.getNode().getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(moveChange.get(), node);
        return true;
    }

    /**
     * @param node The node to move
     * @return true if move was successful, false if not.
     */
    public boolean moveNodeRight(GroupTreeNodeViewModel node, boolean checkSingleSelection) {
        if (checkSingleSelection && (groupsTree.getSelectionCount() != 1)) {
            frame.output(MOVE_ONE_GROUP);
            return false; // not possible
        }
        Optional<MoveGroupChange> moveChange;
        if (!node.canMoveRight() || (! (moveChange = node.moveRight()).isPresent())) {
            frame.output(Localization.lang("Cannot move group \"%0\" right.", node.getNode().getGroup().getName()));
            return false; // not possible
        }
        // update selection/expansion state
        revalidateGroups(groupsTree.refreshPaths(groupsTree.getSelectionPaths()),
                groupsTree.refreshPaths(getExpandedPaths()));
        concludeMoveGroup(moveChange.get(), node);
        return true;
    }

    /**
     * Concludes the moving of a group tree node by storing the specified undo information, marking the change, and
     * setting the status line.
     *
     * @param moveChange Undo information for the move operation.
     * @param node The node that has been moved.
     */
    public void concludeMoveGroup(MoveGroupChange moveChange, GroupTreeNodeViewModel node) {
        panel.undoManager.addEdit(new UndoableMoveGroup(this.groupsRoot, moveChange));
        panel.markBaseChanged();
        frame.output(Localization.lang("Moved group \"%0\".", node.getNode().getGroup().getName()));
    }

    public void concludeAssignment(AbstractUndoableEdit undo, GroupTreeNode node, int assignedEntries) {
        if (undo == null) {
            frame.output(Localization.lang("The group \"%0\" already contains the selection.",
                    node.getGroup().getName()));
            return;
        }
        panel.undoManager.addEdit(undo);
        panel.markBaseChanged();
        panel.updateEntryEditorIfShowing();
        final String groupName = node.getGroup().getName();
        if (assignedEntries == 1) {
            frame.output(Localization.lang("Assigned 1 entry to group \"%0\".", groupName));
        } else {
            frame.output(Localization.lang("Assigned %0 entries to group \"%1\".", String.valueOf(assignedEntries),
                    groupName));
        }
    }



    public GroupTreeNodeViewModel getGroupTreeRoot() {
        return groupsRoot;
    }

    public Enumeration<TreePath> getExpandedPaths() {
        return groupsTree.getExpandedDescendants(groupsRoot.getTreePath());
    }

    /**
     * panel may be null to indicate that no file is currently open.
     */
    @Override
    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel == null) { // hide groups
            frame.getSidePaneManager().hide("groups");
            return;
        }
        MetaData metaData = panel.getBibDatabaseContext().getMetaData();
        if (metaData.getGroups() == null) {
            GroupTreeNode newGroupsRoot = new GroupTreeNode(new AllEntriesGroup());
            metaData.setGroups(newGroupsRoot);
            setGroups(newGroupsRoot);
        } else {
            setGroups(metaData.getGroups());
        }

        // auto show/hide groups interface
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_AUTO_SHOW) && !groupsRoot.isLeaf()) { // groups were defined
            frame.getSidePaneManager().show("groups");
            frame.groupToggle.setSelected(true);
        } else if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_AUTO_HIDE) && groupsRoot.isLeaf()) { // groups were not defined
            frame.getSidePaneManager().hide("groups");
            frame.groupToggle.setSelected(false);
        }

        synchronized (getTreeLock()) {
            validateTree();
        }

    }

    /**
     * Highlight all groups that contain any/all of the specified entries. If entries is null or has zero length,
     * highlight is cleared.
     */
    public void showMatchingGroups(List<BibEntry> list, boolean requireAll) {
        if ((list == null) || (list.isEmpty())) { // nothing selected
            groupsTree.setHighlight3Cells(null);
            groupsTree.revalidate();
            return;
        }
        List<GroupTreeNode> nodeList = groupsRoot.getNode().getContainingGroups(list, requireAll);
        groupsTree.setHighlight3Cells(nodeList.toArray());
        // ensure that all highlighted nodes are visible
        for (GroupTreeNode node : nodeList) {
            node.getParent().ifPresent(
                    parentNode -> groupsTree.expandPath(new GroupTreeNodeViewModel(parentNode).getTreePath()));
        }
        groupsTree.revalidate();
    }

    /**
     * Show groups that, if selected, would show at least one of the entries found in the specified search.
     */
    private void showOverlappingGroups(List<BibEntry> matches) { //DatabaseSearch search) {
        List<GroupTreeNode> nodes = groupsRoot.getNode().getMatchingGroups(matches);
        groupsTree.setHighlight2Cells(nodes.toArray());
    }

    public GroupsTree getGroupsTree() {
        return this.groupsTree;
    }

}
