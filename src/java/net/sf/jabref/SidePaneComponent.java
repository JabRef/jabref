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
package net.sf.jabref;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import com.jgoodies.uif_lite.panel.SimpleInternalFrame;

public abstract class SidePaneComponent extends SimpleInternalFrame {

	protected JButton close = new JButton(GUIGlobals.getImage("close"));

	protected boolean visible = false;

	protected SidePaneManager manager;

	protected BasePanel panel = null;

	public SidePaneComponent(SidePaneManager manager, URL icon, String title) {
		super(new ImageIcon(icon), title);
		this.manager = manager;
		setSelected(true);
		JToolBar tlb = new JToolBar();
		close.setMargin(new Insets(0, 0, 0, 0));
		// tlb.setOpaque(false);
		close.setBorder(null);
        tlb.setFloatable(false);
		tlb.add(close);
		close.addActionListener(new CloseButtonListener());
		setToolBar(tlb);
		// setBorder(BorderFactory.createEtchedBorder());

		// setBorder(BorderFactory.createMatteBorder(1,1,1,1,java.awt.Color.green));
		// setPreferredSize(new java.awt.Dimension
		// (GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 200));
		// Util.pr(""+GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION);
	}

	public void hideAway() {
		manager.hideComponent(this);
	}

	/**
	 * Used by SidePaneManager only, to keep track of visibility.
	 * 
	 */
	void setVisibility(boolean vis) {
		visible = vis;
	}

	/**
	 * Used by SidePaneManager only, to keep track of visibility.
	 * 
	 */
	boolean hasVisibility() {
		return visible;
	}

	public void setActiveBasePanel(BasePanel panel) {
		this.panel = panel;
	}

	public BasePanel getActiveBasePanel() {
		return panel;
	}

	/**
	 * Override this method if the component needs to make any changes before it
	 * can close.
	 */
	public void componentClosing() {

	}

	/**
	 * Override this method if the component needs to do any actions when
	 * opening.
	 */
	public void componentOpening() {

	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	class CloseButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			hideAway();
		}
	}
}
