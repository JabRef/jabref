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
package net.sf.jabref.gui.components;

import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * A JPanel that by default uses a BoxLayout.X_AXIS
 */
public class JPanelXBox extends JPanel {
	/** Create the panel and set BoxLayout.X_AXIS */
	public JPanelXBox() {
		super();
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
	}
	public JPanelXBox(Component comp) {
		this();
		add(comp);
	}
}
