package net.sf.jabref.gui.components;

import java.awt.Dimension;

public class JPanelYBoxPreferredWidth extends JPanelYBox {
	public Dimension getMaximumSize() {
		Dimension pref = getPreferredSize();
		pref.height = super.getMaximumSize().height;
		return pref;				
	}
}

