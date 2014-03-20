package net.sf.jabref.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.BaseAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.groups.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: alver
 * Date: 1/22/13
 * Time: 6:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroupAddRemoveDialog extends BaseAction {

    private BasePanel panel;
    private boolean add, move = false;
    private BibtexEntry[] selection = null;
    JTree tree;
    JButton ok;

    public GroupAddRemoveDialog(BasePanel panel, boolean add, boolean move) {
        this.panel = panel;
        this.add = add;
        this.move = move;
    }

    @Override
    public void action() throws Throwable {
        GroupTreeNode groups = panel.metaData().getGroups();
        if (groups == null) {
            return;
        }

        selection = panel.getSelectedEntries();

        final JDialog diag = new JDialog(panel.frame(),
                Globals.lang(add ? (move ? "Move to group" : "Add to group" )
                        : "Remove from group"), true);
        ok = new JButton(Globals.lang("Ok"));
        JButton cancel = new JButton(Globals.lang("Cancel"));
        tree = new JTree(groups);
        tree.setCellRenderer(new AddRemoveGroupTreeCellRenderer());
        tree.setVisibleRowCount(22);
        tree.setPreferredSize(new Dimension(200, tree.getPreferredSize().height));
        //tree.setSelectionModel(new VetoableTreeSelectionModel());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new SelectionListener());
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (doAddOrRemove())
                    diag.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                diag.dispose();
            }
        });
        ok.setEnabled(false);

        JScrollPane sp = new JScrollPane(tree);

        // Key bindings:
        ActionMap am = sp.getActionMap();
        InputMap im = sp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", new AbstractAction() {
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

    class SelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            GroupTreeNode node = (GroupTreeNode)e.getNewLeadSelectionPath().getLastPathComponent();
            AbstractGroup group = node.getGroup();
            ok.setEnabled(checkGroupEnable(group));
        }
    }

    protected boolean doAddOrRemove() {
        GroupTreeNode node = (GroupTreeNode)tree.getSelectionPath().getLastPathComponent();
        AbstractGroup group = node.getGroup();
        if (checkGroupEnable(group)) {

            if (add) {
                AddToGroupAction action = new AddToGroupAction(node, move, panel);
                action.actionPerformed(new ActionEvent(node, 0, "add"));
            } else {
                RemoveFromGroupAction action = new RemoveFromGroupAction(node, panel);
                action.actionPerformed(new ActionEvent(node, 0, "remove"));
            }

            return true;
        }
        else {
            return false;
        }

    }

    /**
     * Check if we can perform the action for this group. Determines whether
     * the group should be shown in an enabled state, and if selecting it should
     * leave the Ok button enabled.
     * @param group The group to check
     * @return true if this dialog's action can be performed on the group
     */
    private boolean checkGroupEnable(AbstractGroup group) {
        return (add ? group.supportsAdd() && !group.containsAll(selection)
                : group.supportsRemove() && group.containsAny(selection));
    }


/*    private class VetoableTreeSelectionModel extends DefaultTreeSelectionModel {

        @Override
        public void addSelectionPath(TreePath path) {
            if (checkPath(path))
                super.addSelectionPath(path);
        }

        public void setSelectionPath(TreePath path){
            if (checkPath(path))
                super.setSelectionPath(path);

        }

        private boolean checkPath(TreePath path) {
            GroupTreeNode node = (GroupTreeNode)path.getLastPathComponent();
            AbstractGroup group = node.getGroup();
            return (add ? group.supportsAdd() && !group.containsAll(GroupAddRemoveDialog.this.selection)
                    : group.supportsRemove() && group.containsAny(GroupAddRemoveDialog.this.selection));
        }
    }
    {

    } */

    class AddRemoveGroupTreeCellRenderer extends GroupTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            GroupTreeNode node = (GroupTreeNode)value;
            AbstractGroup group = node.getGroup();
            if (checkGroupEnable(group))
                c.setForeground(Color.black);
            else
                c.setForeground(Color.gray);

            return c;
        }
    }


}


