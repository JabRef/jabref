package net.sf.jabref.gui.components;

import java.awt.Dimension;

public class JPanelYBoxPreferredHeight extends JPanelYBox {
	public Dimension getMaximumSize() {
		Dimension pref = getPreferredSize();
		pref.width = super.getMaximumSize().width;
		return pref;				
	}
}

