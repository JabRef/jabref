package net.sf.jabref.groups;

import java.awt.Component;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;


/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its
 * toString() method.
 * 
 * @author zieren
 */
public class GroupTreeCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        Component c = super.getTreeCellRendererComponent(tree, value, selected,
                expanded, leaf, row, hasFocus);
        AbstractGroup group = ((GroupTreeNode) value).getGroup();
        if (c instanceof JLabel) { // sanity check
            ((JLabel) c).setText(group.getName());
            ((JLabel) c).setToolTipText(group.getName());
        }
        return c;
    }
}
