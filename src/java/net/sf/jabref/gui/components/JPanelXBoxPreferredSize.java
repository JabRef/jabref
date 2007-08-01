package net.sf.jabref.gui.components;

import java.awt.Component;
import java.awt.Dimension;

public class JPanelXBoxPreferredSize extends JPanelXBox {
	public JPanelXBoxPreferredSize() {
		// nothing special
	}
	public JPanelXBoxPreferredSize(Component c) {
		add(c);
	}
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
}

