/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer for table cells, which supports both Icons, JLabels and plain text.
 */
public class GeneralRenderer extends DefaultTableCellRenderer {

    private final Color rendererBackground;
    private Color selBackground;

    public GeneralRenderer(Color c) {
        super();
        this.rendererBackground = c;
        setBackground(c);
    }

    /**
     * Renderer with specified foreground and background colors, and default selected
     * background color.
     * @param c Foreground color
     * @param fg Background color
     */
    public GeneralRenderer(Color c, Color fg) {
        this(c);
        setForeground(fg);
    }

    /**
     * Renderer with specified foreground, background and selected background colors
     * @param c Foreground color
     * @param fg Unselected background color
     * @param sel Selected background color
     */
    public GeneralRenderer(Color c, Color fg, Color sel) {
        this(c, fg);
        this.selBackground = sel;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object o, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if (selBackground == null) {
            return super.getTableCellRendererComponent(table, o, isSelected, hasFocus, row, column);
        } else {
            Component c = super.getTableCellRendererComponent(table, o, isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setBackground(selBackground);
            } else {
                c.setBackground(rendererBackground);
            }
            return c;
        }
    }

    @Override
    public void firePropertyChange(String propertyName, Object old, Object newV) {
        // disable super.firePropertyChange
    }

    /* For enabling the renderer to handle icons. */
    @Override
    protected void setValue(Object value) {
        if (value instanceof Icon) {
            setIcon((Icon) value);
            setText(null);
        } else if (value instanceof JLabel) {
            JLabel lab = (JLabel) value;
            setIcon(lab.getIcon());
            setToolTipText(lab.getToolTipText());
            if (lab.getIcon() != null) {
                setText(null);
            }
        } else {
            // this is plain text
            setIcon(null);
            setToolTipText(null);
            if (value == null) {
                setText(null);
            } else {
                setText(value.toString());
            }
        }
    }

}
