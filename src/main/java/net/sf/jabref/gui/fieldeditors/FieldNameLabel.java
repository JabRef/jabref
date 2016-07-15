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
package net.sf.jabref.gui.fieldeditors;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.model.entry.EntryUtil;

public class FieldNameLabel extends JLabel {

    public FieldNameLabel(String name) {
        super(FieldNameLabel.getFieldNameLabelText(name), SwingConstants.LEFT);

        setVerticalAlignment(SwingConstants.TOP);
        setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        super.paintComponent(g2);
    }

    private static String getFieldNameLabelText(String fieldName) {
        // selected terms should be uppercase
        if("isbn".equalsIgnoreCase(fieldName)) {
            return " ISBN ";
        } else if ("url".equalsIgnoreCase(fieldName)){
            return " URL ";
        } else if ("uri".equalsIgnoreCase(fieldName)) {
            return " URI ";
        } else if ("issn".equalsIgnoreCase(fieldName)) {
            return " ISSN ";
        } else if("doi".equalsIgnoreCase(fieldName)) {
            return " DOI ";
        } else if("isrn".equalsIgnoreCase(fieldName)) {
            return " ISRN ";
        }

        // otherwise capitalize
        return ' ' + EntryUtil.capitalizeFirst(fieldName) + ' ';
    }


}
