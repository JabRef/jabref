package net.sf.jabref.gui.components;

import java.awt.*;

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

