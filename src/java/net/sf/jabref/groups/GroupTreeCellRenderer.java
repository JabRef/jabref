/*
All programs in this directory and subdirectories are published under the 
GNU General Public License as described below.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 2 of the License, or (at your option) 
any later version.

This program is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
more details.

You should have received a copy of the GNU General Public License along 
with this program; if not, write to the Free Software Foundation, Inc., 59 
Temple Place, Suite 330, Boston, MA 02111-1307 USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html
*/

package net.sf.jabref.groups;

import java.awt.Component;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;


/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its
 * toString() method.
 * 
 * @author jzieren
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
            ((JLabel) c).setIcon(null); // save some space
        }
        return c;
    }
}
