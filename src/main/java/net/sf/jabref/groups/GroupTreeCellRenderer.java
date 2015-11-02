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
package net.sf.jabref.groups;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.groups.structure.*;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its toString() method.
 *
 * @author jzieren
 */
public class GroupTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final int MAX_DISPLAYED_LETTERS = 35;

    /** The cell over which the user is currently dragging */
    private Object highlight1Cell;
    private Object[] highlight2Cells;
    private Object[] highlight3Cells;
    private Object highlightBorderCell;

    private static final Icon groupRefiningIcon = IconTheme.JabRefIcon.GROUP_REFINING.getSmallIcon();
    private static final Icon groupIncludingIcon = IconTheme.JabRefIcon.GROUP_INCLUDING.getSmallIcon();
    private static final Icon groupRegularIcon = null;


    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean tmpHasFocus) {
        if (value == highlight1Cell) {
            selected = true; // show as selected
        } else {
            selected = sel;
        }
        Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, tmpHasFocus);
        // this is sometimes called from deep within somewhere, with a dummy
        // value (probably for layout etc.), so we've got to check here!
        if (!(value instanceof GroupTreeNode)) {
            return c;
        }
        AbstractGroup group = ((GroupTreeNode) value).getGroup();
        if ((group == null) || !(c instanceof JLabel)) {
            return c; // sanity check
        }
        JLabel label = (JLabel) c;

        if ((highlightBorderCell != null) && (highlightBorderCell == value)) {
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else {
            label.setBorder(BorderFactory.createEmptyBorder());
        }
        boolean italics = Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC) && group.isDynamic();
        boolean red = false;
        if (highlight2Cells != null) {
            for (Object highlight2Cell : highlight2Cells) {
                if (highlight2Cell == value) {
                    // label.setForeground(Color.RED);
                    red = true;
                    break;
                }
            }
        }
        boolean underline = false;
        if (highlight3Cells != null) {
            for (Object highlight3Cell : highlight3Cells) {
                if (highlight3Cell == value) {
                    underline = true;
                    break;
                }
            }
        }
        String name = group.getName();
        if (name.length() > GroupTreeCellRenderer.MAX_DISPLAYED_LETTERS) {
            name = name.substring(0, GroupTreeCellRenderer.MAX_DISPLAYED_LETTERS - 2) + "...";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (red) {
            sb.append("<font color=\"#FF0000\">");
        }
        if (underline) {
            sb.append("<u>");
        }
        if (italics) {
            sb.append("<i>");
        }
        sb.append(StringUtil.quoteForHTML(name));
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS)) {
            if (group instanceof ExplicitGroup) {
                sb.append(" [").append(((ExplicitGroup) group).getNumEntries()).append("]");
            } else if ((group instanceof KeywordGroup) || (group instanceof SearchGroup)) {
                int hits = 0;
                for (BibtexEntry entry : JabRef.jrf.basePanel().getDatabase().getEntries()) {
                    if (group.contains(entry)) {
                        hits++;
                    }
                }
                sb.append(" [").append(hits).append("]");
            }
        }
        if (italics) {
            sb.append("</i>");
        }
        if (underline) {
            sb.append("</u>");
        }
        if (red) {
            sb.append("</font>");
        }
        sb.append("</html>");
        final String text = sb.toString();

        if (!label.getText().equals(text)) {
            label.setText(text);
        }
        label.setToolTipText("<html>" + group.getShortDescription() + "</html>");
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_ICONS)) {
            switch (group.getHierarchicalContext()) {
            case REFINING:
                if (label.getIcon() != GroupTreeCellRenderer.groupRefiningIcon) {
                    label.setIcon(GroupTreeCellRenderer.groupRefiningIcon);
                }
                break;
            case INCLUDING:
                if (label.getIcon() != GroupTreeCellRenderer.groupIncludingIcon) {
                    label.setIcon(GroupTreeCellRenderer.groupIncludingIcon);
                }
                break;
            default:
                if (label.getIcon() != GroupTreeCellRenderer.groupRegularIcon) {
                    label.setIcon(GroupTreeCellRenderer.groupRegularIcon);
                }
                break;
            }
        } else {
            label.setIcon(null);
        }
        return c;
    }

    /**
     * For use when dragging: The specified cell is always rendered as selected.
     *
     * @param cell The cell over which the user is currently dragging.
     */
    void setHighlight1Cell(Object cell) {
        this.highlight1Cell = cell;
    }

    /**
     * Highlights the specified cells (in red), or disables highlight if cells == null.
     */
    void setHighlight2Cells(Object[] cells) {
        this.highlight2Cells = cells;
    }

    /**
     * Highlights the specified cells (by underlining), or disables highlight if cells == null.
     */
    void setHighlight3Cells(Object[] cells) {
        this.highlight3Cells = cells;
    }

    /**
     * Highlights the specified cells (by drawing a border around it), or disables highlight if highlightBorderCell ==
     * null.
     */
    void setHighlightBorderCell(Object highlightBorderCell) {
        this.highlightBorderCell = highlightBorderCell;
    }
}
