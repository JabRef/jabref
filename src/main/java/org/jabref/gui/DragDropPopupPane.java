package org.jabref.gui;

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
