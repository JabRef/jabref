package net.sf.jabref.gui.components;

import java.awt.*;

public class JPanelYBoxPreferredSize extends JPanelYBox {
	public JPanelYBoxPreferredSize() {
		// nothing special
	}
	public JPanelYBoxPreferredSize(Component c) {
		add(c);
	}
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
}

