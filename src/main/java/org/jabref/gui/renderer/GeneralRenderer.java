package org.jabref.gui.renderer;

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
