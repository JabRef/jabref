/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.gui;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Adds popup functionality to DragDropPane
 */
public class DragDropPopupPane extends DragDropPane {

    private final JPopupMenu popupMenu;

    public DragDropPopupPane(JPopupMenu menu) {
        this.popupMenu = menu;

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tabRightClick(e);
            }
        });
    }

    private void tabRightClick(MouseEvent e) {
        if ((e.getButton() != MouseEvent.BUTTON1) && (e.getClickCount() == 1)) {
            // display popup near location of mouse click
            popupMenu.show(e.getComponent(), e.getX(), e.getY() - 10);
        }
    }
}
