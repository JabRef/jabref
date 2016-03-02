/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.gui.maintable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MainTableHeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer delegate;

    public MainTableHeaderRenderer(TableCellRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        // delegate to previously used TableCellRenderer which styles the component
        Component resultFromDelegate = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Changing style is only possible if both value and resultFromDelegate are JLabels
        if ((value instanceof JLabel) && (resultFromDelegate instanceof JLabel)) {
            String text = ((JLabel) value).getText();
            Icon icon = ((JLabel) value).getIcon();
            if (icon == null) {
                ((JLabel) resultFromDelegate).setText(text);
            } else {
                ((JLabel) resultFromDelegate).setIcon(icon);
                ((JLabel) resultFromDelegate).setText(null);
            }
        }

        return resultFromDelegate;
    }
}
