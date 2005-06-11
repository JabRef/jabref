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

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.jabref.*;


/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its
 * toString() method.
 * 
 * @author jzieren
 */
public class GroupTreeCellRenderer extends DefaultTreeCellRenderer {
    /** The cell over which the user is currently dragging */
    protected Object highlight1Cell = null;
    protected Object[] highlight2Cells = null;
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        if (value == highlight1Cell)
            selected = true; // show as selected
        Component c = super.getTreeCellRendererComponent(tree, value, selected,
                expanded, leaf, row, hasFocus);
        AbstractGroup group = ((GroupTreeNode) value).getGroup();
        if (group == null || !(c instanceof JLabel))
            return c; // sanity check
        JLabel label = (JLabel)c;
        if (highlight2Cells != null) {
            for (int i = 0; i < highlight2Cells.length; ++i) {
                if (highlight2Cells[i] == value) {
                    label.setForeground(Color.RED);
                    break;
                }
            }
        }
        if (!label.getText().equals(group.getName())) {
        	label.setText(group.getName());
        	label.setToolTipText("<html>" + group.getShortDescription()
        			+ "</html>");
        }
        if (Globals.prefs.getBoolean("groupShowIcons")) {
	        switch (group.getHierarchicalContext()) {
	        case AbstractGroup.REFINING:
	        	if (label.getIcon() != GUIGlobals.groupRefiningIcon)
	        		label.setIcon(GUIGlobals.groupRefiningIcon);
	        	break;
	        case AbstractGroup.INCLUDING:
	        	if (label.getIcon() != GUIGlobals.groupIncludingIcon)
	        		label.setIcon(GUIGlobals.groupIncludingIcon);
	        	break;
	        default:
	        	if (label.getIcon() != GUIGlobals.groupRegularIcon)
	        		label.setIcon(GUIGlobals.groupRegularIcon);
	        	break;
	        }
        } else {
        	label.setIcon(null);
        }
        if (Globals.prefs.getBoolean("groupShowDynamic")) {
        	Font f = label.getFont();
        	if (group.isDynamic()) {
	        	if (!f.isItalic()) {
	        		f = f.deriveFont(Font.ITALIC);
	        		label.setFont(f);
	        	}
        	} else {
	        	if (f.isItalic()) {
	        		f = f.deriveFont(Font.PLAIN);
	        		label.setFont(f);
	        	}
        	}
        }
        return c;
    }
    /** 
     * For use when dragging: The sepcified cell is always rendered as
     * selected. 
     * @param cell The cell over which the user is currently dragging.
     */ 
    void setHighlight1Cell(Object cell) {
        this.highlight1Cell = cell;
    }
    /**
     * Highlights the specified cells (in red), or disables highlight if cells ==
     * null.
     */ 
    void setHighlight2Cells(Object[] cells) {
        this.highlight2Cells = cells;
    }
}
