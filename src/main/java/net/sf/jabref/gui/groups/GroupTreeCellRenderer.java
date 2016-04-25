/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its toString() method.
 *
 * @author jzieren
 */
public class GroupTreeCellRenderer extends DefaultTreeCellRenderer {

    /** The cell over which the user is currently dragging */
    private Object highlight1Cell;
    private Object[] highlight2Cells;
    private Object[] highlight3Cells;
    private Object highlightBorderCell;


    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean tmpHasFocus) {
        // show as selected
        selected = ((highlight1Cell != null) && highlight1Cell.equals(value)) || sel;
        Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, tmpHasFocus);
        // this is sometimes called from deep within somewhere, with a dummy
        // value (probably for layout etc.), so we've got to check here!
        if (!(value instanceof GroupTreeNodeViewModel)) {
            return c;
        }
        if (!(c instanceof JLabel)) {
            return c; // sanity check
        }

        GroupTreeNodeViewModel viewModel = (GroupTreeNodeViewModel) value;
        JLabel label = (JLabel) c;

        if ((highlightBorderCell != null) && (highlightBorderCell.equals(value))) {
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else {
            label.setBorder(BorderFactory.createEmptyBorder());
        }

        Boolean red = printInRed(value);
        Boolean underlined = printUnderlined(value);
        StringBuilder sb = new StringBuilder(60);
        sb.append("<html>");
        if (red) {
            sb.append("<font color=\"#FF0000\">");
        }
        if (underlined) {
            sb.append("<u>");
        }
        if (viewModel.printInItalics()) {
            sb.append("<i>");
        }
        sb.append(StringUtil.quoteForHTML(viewModel.getText()));
        if (viewModel.printInItalics()) {
            sb.append("</i>");
        }
        if (underlined) {
            sb.append("</u>");
        }
        if (red) {
            sb.append("</font>");
        }
        sb.append("</html>");

        String text = sb.toString();
        if (!label.getText().equals(text)) {
            label.setText(text);
        }
        label.setToolTipText(viewModel.getDescription());

        Icon icon = viewModel.getIcon();
        if (label.getIcon() != icon) {
            label.setIcon(icon);
        }
        return c;
    }

    private boolean printInRed(Object value) {
        if (highlight2Cells != null) {
            for (Object highlight2Cell : highlight2Cells) {
                if (highlight2Cell.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean printUnderlined(Object value) {
        if (highlight3Cells != null) {
            for (Object highlight3Cell : highlight3Cells) {
                if (highlight3Cell.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * For use when dragging: The specified cell is always rendered as selected.
     *
     * @param cell The cell over which the user is currently dragging.
     */
    public void setHighlight1Cell(Object cell) {
        this.highlight1Cell = cell;
    }

    /**
     * Highlights the specified cells (in red), or disables highlight if cells == null.
     */
    public void setHighlight2Cells(Object[] cells) {
        this.highlight2Cells = cells;
    }

    /**
     * Highlights the specified cells (by underlining), or disables highlight if cells == null.
     */
    public void setHighlight3Cells(Object[] cells) {
        this.highlight3Cells = cells;
    }

    /**
     * Highlights the specified cells (by drawing a border around it), or disables highlight if highlightBorderCell ==
     * null.
     */
    public void setHighlightBorderCell(Object highlightBorderCell) {
        this.highlightBorderCell = highlightBorderCell;
    }
}
