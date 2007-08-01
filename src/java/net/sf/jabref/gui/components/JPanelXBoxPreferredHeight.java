package net.sf.jabref.gui.components;

import java.awt.Component;
import java.awt.Dimension;

public class JPanelXBoxPreferredHeight extends JPanelXBox {
	public JPanelXBoxPreferredHeight() {
		// nothing special
	}
	public JPanelXBoxPreferredHeight(Component c) {
		add(c);
	}
	public Dimension getMaximumSize() {
		Dimension pref = getPreferredSize();
		pref.width = super.getMaximumSize().width;
		return pref;				
	}
}

