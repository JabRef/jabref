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

package net.sf.jabref.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.*;

/**
 * Extends the JTabbedPane class to support Drag&Drop of Tabs.
 *
 * @author kleinms, strassfn
 */
class DragDropPane extends JTabbedPane {

    private boolean draggingState; // State var if we are at dragging or not
    private int indexDraggedTab; // The index of the tab we drag at the moment
    private final MarkerPane markerPane; // The glass panel for painting the position marker


    DragDropPane() {
        super();
        indexDraggedTab = -1;
        markerPane = new MarkerPane();
        markerPane.setVisible(false);

        // -------------------------------------------
        // Adding listeners for Drag&Drop Actions
        // -------------------------------------------
        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) { // Mouse is dragging
                // Calculates the tab index based on the mouse position
                int indexActTab = getUI().tabForCoordinate(DragDropPane.this,
                        e.getX(), e.getY());
                if (draggingState) { // We are at tab dragging
                    if ((indexDraggedTab >= 0) && (indexActTab >= 0)) { //Is it a valid scenario?
                        boolean toTheLeft = e.getX() <= getUI().getTabBounds(DragDropPane.this, indexActTab).getCenterX(); //Go to the left or to the right of the actual Tab
                        DragDropPane.this.getRootPane().setGlassPane(markerPane); //Set the MarkerPane as glass Pane
                        Rectangle actTabRect = SwingUtilities.convertRectangle(DragDropPane.this, getBoundsAt(indexActTab),
                                DragDropPane.this.markerPane); //Rectangle with the same dimensions as the tab at the mouse position
                        if (toTheLeft) {
                            markerPane.setPicLocation(new Point(actTabRect.x, actTabRect.y
                                    + actTabRect.height)); //Set pic to the left of the tab at the mouse position
                        }
                        else {
                            markerPane.setPicLocation(new Point(actTabRect.x + actTabRect.width, actTabRect.y
                                    + actTabRect.height)); //Set pic to the right of the tab at the mouse position
                        }

                        markerPane.setVisible(true);
                        markerPane.repaint();
                        repaint();
                    } else { //We have no valid tab tragging scenario
                        markerPane.setVisible(false);
                        markerPane.repaint();
                    }

                } else { //We are not at tab dragging
                    if (indexActTab >= 0) { // Mouse is above a tab, otherwise tabNumber would be -1
                        // -->Now we are at tab tragging
                        draggingState = true; // Mark now we are at dragging
                        indexDraggedTab = indexActTab; // Set draggedTabIndex to the tabNumber where we are now
                        repaint();
                    }
                }
                super.mouseDragged(e);
            }
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                DragDropPane.this.markerPane.setVisible(false); //Set MarkerPane invisible
                int indexActTab = getUI().tabForCoordinate(DragDropPane.this,
                        e.getX(), e.getY());
                if ((indexDraggedTab >= 0) && (indexActTab >= 0) && (indexDraggedTab != indexActTab)) { //Is it a valid scenario?
                    if (draggingState) { //We are at tab dragging
                        boolean toTheLeft = e.getX() <= getUI().getTabBounds(DragDropPane.this, indexActTab).getCenterX(); //Go to the left or to the right of the actual Tab
                        DragDropPane.this.markerPane.setVisible(false);

                        Component actTab = getComponentAt(indexDraggedTab); //Save dragged tab
                        String actTabTitle = getTitleAt(indexDraggedTab); //Save Title of the dragged tab
                        removeTabAt(indexDraggedTab); //Remove dragged tab
                        int newTabPos;
                        if (indexActTab < indexDraggedTab) { //We are dragging the tab to the left of its the position
                            if (toTheLeft && (indexActTab < (DragDropPane.this.getTabCount()))) {
                                newTabPos = indexActTab;
                            } else {
                                newTabPos = indexActTab + 1;
                            }
                        } else { //We are dragging the tab to the right of the old position
                            if (toTheLeft && (indexActTab > 0)) {
                                newTabPos = indexActTab - 1;
                            } else {
                                newTabPos = indexActTab;
                            }
                        }
                        insertTab(actTabTitle, null, actTab, null, newTabPos); //Insert dragged tab at new position
                        DragDropPane.this.setSelectedIndex(newTabPos); //Set selection back to the tab (at the new tab position
                    }
                }
                draggingState = false;
            }
        });
    }


    /**
     * A glass panel which sets the marker for Dragging of Tabs.
     *
     */
    static class MarkerPane extends JPanel {

        private Point locationP;
        private final IconTheme.JabRefIcon moveTabArrow;


        public MarkerPane() {
            setOpaque(false);

            // Sets the marker fontIcon
            moveTabArrow = IconTheme.JabRefIcon.MOVE_TAB_ARROW;
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.9f)); // Set transparency
            g.setFont(IconTheme.FONT.deriveFont(Font.BOLD, 24f));
            g.drawString(moveTabArrow.getCode(), locationP.x - (moveTabArrow.getIcon().getIconWidth() / 2),
                    locationP.y + (moveTabArrow.getIcon().getIconHeight() / 2));

        }

        /**
         * Sets the new location, where the marker should be placed.
         *
         * @param pt the point for the marker
         */
        public void setPicLocation(Point pt) {
            this.locationP = pt;
        }

    }
}