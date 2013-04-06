package net.sf.jabref.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.BaseAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.GroupTreeCellRenderer;
import net.sf.jabref.groups.GroupTreeNode;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: alver
 * Date: 1/22/13
 * Time: 6:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroupAddRemoveDialog extends BaseAction {

    private BasePanel panel;
    private boolean add;

    public GroupAddRemoveDialog(BasePanel panel, boolean add) {
        this.panel = panel;

        this.add = add;
    }
    @Override
    public void action() throws Throwable {
        GroupTreeNode groups = panel.metaData().getGroups();
        if (groups == null) {
            return;
        }

        JDialog diag = new JDialog(panel.frame(),
                Globals.lang(add ? "Add to group" : "Remove from group"), true);
        JButton ok = new JButton(Globals.lang("Ok")),
                cancel = new JButton(Globals.lang("Cancel"));
        JTree tree = new JTree(groups);
        tree.setCellRenderer(new GroupTreeCellRenderer());
        tree.setVisibleRowCount(18);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        diag.getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(panel.frame());
        diag.setVisible(true);


    }

    /**
     * @param move For add: if true, remove from previous groups

    public void insertNodes(JMenu menu, GroupTreeNode node, BibtexEntry[] selection,
                            boolean add, boolean move) {
        final AbstractAction action = getAction(node,selection,add,move);

        if (node.getChildCount() == 0) {
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            menu.add(menuItem);
            if (action.isEnabled())
                menu.setEnabled(true);
            return;
        }

        JMenu submenu = null;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu,(GroupTreeNode) node.getChildAt(i), selection, add, move);
            }
        } else {
            submenu = new JMenu("["+node.getGroup().getName()+"]");
            setGroupFontAndIcon(submenu, node.getGroup());
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            submenu.add(menuItem);
            submenu.add(new JPopupMenu.Separator());
            for (int i = 0; i < node.getChildCount(); ++i)
                insertNodes(submenu,(GroupTreeNode) node.getChildAt(i), selection, add, move);
            menu.add(submenu);
            if (submenu.isEnabled())
                menu.setEnabled(true);
        }
    }
     */
}
