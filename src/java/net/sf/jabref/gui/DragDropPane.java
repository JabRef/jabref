/*
 * Adds the functionality for Drag&Drop of Tabs to a JTabbedPane.
 * @author kleinms, strassfn
 */

package net.sf.jabref.gui;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.GUIGlobals;

/**
 * Extends the JTabbedPane class to support Drag&Drop of Tabs.
 * 
 */
public class DragDropPane extends JTabbedPane {
	private boolean draggingState = false; // State var if we are at dragging or not
	private int indexDraggedTab; // The index of the tab we drag at the moment
	MarkerPane markerPane; // The glass panel for painting the position marker

	public DragDropPane() {
		super();
		indexDraggedTab = -1;
		markerPane = new MarkerPane();
		markerPane.setVisible(false);

		// -------------------------------------------
		// Adding listeners for Drag&Drop Actions
		// -------------------------------------------
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) { // Mouse is dragging
											// Calculates the tab index based on the mouse position
				int indexActTab = getUI().tabForCoordinate(DragDropPane.this,
						e.getX(), e.getY());
				if (!draggingState) { // We are not at tab dragging
					if (indexActTab >= 0) { // Mouse is above a tab, otherwise tabNumber would be -1
											// -->Now we are at tab tragging
						draggingState = true; // Mark now we are at dragging
						indexDraggedTab = indexActTab; // Set draggedTabIndex to the tabNumber where we are now
						repaint();
					}

				} else { //We are at tab tragging
					if (indexDraggedTab >= 0 && indexActTab >= 0 && indexDraggedTab != indexActTab) { //Is it a valid scenario?
						DragDropPane.this.getRootPane().setGlassPane(markerPane); //Set the MarkerPane as glass Pane
						Rectangle actTabRect = SwingUtilities.convertRectangle(DragDropPane.this, getBoundsAt(indexActTab),
								DragDropPane.this.markerPane); //Rectangle with the same dimensions as the tab at the mouse position
						if (indexDraggedTab < indexActTab)
							markerPane.setPicLocation(new Point(actTabRect.x + actTabRect.width, actTabRect.y
									+ actTabRect.height)); //Set pic to the right of the tab at the mouse position
						else if (indexDraggedTab > indexActTab)
							markerPane.setPicLocation(new Point(actTabRect.x, actTabRect.y + actTabRect.height)); //Set pic to the left of the tab at the mouse position

						markerPane.setVisible(true);
						markerPane.repaint();
						repaint();
					} else { //We have no valid tab tragging scenario
						markerPane.setVisible(false);
						markerPane.repaint();
					}
				}
				super.mouseDragged(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				DragDropPane.this.markerPane.setVisible(false); //Set MarkerPane invisible
				int indexActTab = getUI().tabForCoordinate(DragDropPane.this,
						e.getX(), e.getY());
				if (indexDraggedTab >= 0 && indexActTab >= 0 && indexDraggedTab != indexActTab) { //Is it a valid scenario?
					if (draggingState) { //We are at tab tragging
						DragDropPane.this.markerPane.setVisible(false);

						Component actTab = getComponentAt(indexDraggedTab); //Save dragged tab
						String actTabTitle = getTitleAt(indexDraggedTab); //Save Title of the dragged tab
						removeTabAt(indexDraggedTab); //Remove dragged tab
						insertTab(actTabTitle, null, actTab, null, indexActTab); //Insert dragged tab at new position
						DragDropPane.this.setSelectedIndex(indexActTab); //Set selection back to the tab (at the new tab position
	
						indexDraggedTab = indexActTab; //Only for robustness
					}
				}
				draggingState = false;
			}
		});
	}

	/**
	 * A glass panel who sets the marker for Dragging of Tabs.
	 * 
	 */
	class MarkerPane extends JPanel {
		private Point locationP;
		private Image markerImg;

		public MarkerPane() {
			setOpaque(false);
			markerImg = Toolkit.getDefaultToolkit().getImage(
					GUIGlobals.getIconUrl("dragNdropArrow")); // Sets the marker image
		}

		@Override
		public void paintComponent(Graphics g) {
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.9f)); // Set transparency
			g.drawImage(markerImg, locationP.x - (markerImg.getWidth(null) / 2),
					locationP.y, null); // draw the image at the middle of the given location
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