package org.jabref.gui.groups;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class GroupAddRemoveDialog implements BaseAction {

    private final BasePanel panel;
    private final boolean add;
    private final boolean move;
    private List<BibEntry> selection;
    private JTree tree;

    public GroupAddRemoveDialog(BasePanel panel, boolean add, boolean move) {
        this.panel = panel;
        this.add = add;
        this.move = move;
    }

    @Override
    public void action() throws Exception {
        Optional<GroupTreeNode> groups = panel.getBibDatabaseContext().getMetaData().getGroups();
        if (!groups.isPresent()) {
            return;
        }

        selection = panel.getSelectedEntries();

        final JDialog diag = new JDialog(panel.frame(),
                (add ? (move ? Localization.lang("Move to group") : Localization.lang("Add to group")) : Localization
                        .lang("Remove from group")),
                true);
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        tree = new JTree(new GroupTreeNodeViewModel(groups.get()));
        tree.setCellRenderer(new AddRemoveGroupTreeCellRenderer());
        tree.setVisibleRowCount(22);

        //        tree.setPreferredSize(new Dimension(200, tree.getPreferredSize().height));
        //      The scrollbar appears when the preferred size of a component is greater than the size of the viewport. If one hard coded the preferred size, it will never change according to the expansion/collapse. Thus the scrollbar cannot appear accordingly.
        //tree.setSelectionModel(new VetoableTreeSelectionModel());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) e.getNewLeadSelectionPath().getLastPathComponent();
            ok.setEnabled(checkGroupEnable(node));
        });

        //STA add expand and collapse all buttons
        JButton jbExpandAll = new JButton("Expand All");

        jbExpandAll.addActionListener(e -> expandAll(tree, true));

        JButton jbCollapseAll = new JButton("Collapse All");
        jbCollapseAll.addActionListener(e -> expandAll(tree, false));
        //END add expand and collapse all buttons

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);

        bb.addButton(jbExpandAll);
        bb.addButton(jbCollapseAll);

        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ok.addActionListener(actionEvent -> {
            if (doAddOrRemove()) {
                diag.dispose();
                tree.repaint();
            }
        });
        cancel.addActionListener(actionEvent -> diag.dispose());
        ok.setEnabled(false);

        JScrollPane sp = new JScrollPane(tree);

        // Key bindings:
        ActionMap am = sp.getActionMap();
        InputMap im = sp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });

        diag.getContentPane().add(sp, BorderLayout.CENTER);

        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(panel.frame());
        diag.setVisible(true);

    }

    // If "expand" is true, all nodes in the tree area expanded
    // otherwise all nodes in the tree are collapsed:
    private void expandAll(final JTree subtree, final boolean expand) {
        SwingUtilities.invokeLater(() -> {
            TreeNode root = ((TreeNode) subtree.getModel().getRoot());
            // walk through the tree, beginning at the root:
            expandAll(subtree, new TreePath(((DefaultTreeModel) subtree.getModel()).getPathToRoot(root)), expand);
            tree.requestFocusInWindow();
        });
    }

    private void expandAll(final JTree subtree, final TreePath parent, final boolean expand) {
        // walk through the children:
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        int numChildren = node.getChildCount();
        if (numChildren > 0) {
            for (int i = 0; i < numChildren; i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                expandAll(subtree, path, expand);
            }
        }
        // "expand" / "collapse" occurs from bottom to top:
        if (expand) {
            tree.expandPath(parent);
        } else {
            if (node.getParent() != null) {
                tree.collapsePath(parent);
            }
        }
    }

    private boolean doAddOrRemove() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return false;
        } else {
            GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
            if (checkGroupEnable(node)) {

                List<BibEntry> entries = Globals.stateManager.getSelectedEntries();

                if (move) {
                    recuriveRemoveFromNode((GroupTreeNodeViewModel) tree.getModel().getRoot(), entries);
                }

                if (add) {
                    node.addEntriesToGroup(entries);
                } else {
                    node.removeEntriesFromGroup(Globals.stateManager.getSelectedEntries());
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private void recuriveRemoveFromNode(GroupTreeNodeViewModel node, List<BibEntry> entries) {
        node.removeEntriesFromGroup(entries);
        for (GroupTreeNodeViewModel child: node.getChildren()) {
            recuriveRemoveFromNode(child, entries);
        }
    }

    /**
     * Check if we can perform the action for this group. Determines whether
     * the group should be shown in an enabled state, and if selecting it should
     * leave the Ok button enabled.
     * @param node The group to check
     * @return true if this dialog's action can be performed on the group
     */
    private boolean checkGroupEnable(GroupTreeNodeViewModel node) {
        return (add ? node.canAddEntries(selection) : node.canRemoveEntries(selection));
    }

    class AddRemoveGroupTreeCellRenderer extends GroupTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) value;
            if (checkGroupEnable(node)) {
                c.setForeground(Color.black);
            } else {
                c.setForeground(Color.gray);
            }

            return c;
        }
    }

}
