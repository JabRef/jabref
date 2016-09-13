package net.sf.jabref.gui.util;

import java.awt.FontMetrics;

import javax.swing.JTable;
import javax.swing.JTree;

public final class GUIUtil {

    private GUIUtil() {
        // Private constructor as all methods are static
    }

    /**
     * Update table row height to the best possible considering font size changes on the current platform
     *
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4760081
     *
     * @param table
     */
    public static void correctRowHeight(JTable table) {
        // Fix tree row height
        FontMetrics metrics = table.getFontMetrics(table.getFont());
        table.setRowHeight(Math.max(table.getRowHeight(), metrics.getHeight()));
    }

    /**
     * Update tree row height to the best possible considering font size changes on the current platform
     *
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4760081
     *
     * @param table
     */
    public static void correctRowHeight(JTree tree) {
        // Fix tree row height
        FontMetrics metrics = tree.getFontMetrics(tree.getFont());
        tree.setRowHeight(Math.max(tree.getRowHeight(), metrics.getHeight()));
    }
}
