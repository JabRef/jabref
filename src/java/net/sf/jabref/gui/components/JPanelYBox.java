package net.sf.jabref.gui.components;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * A JPanel that by default uses a BoxLayout.Y_AXIS
 */
public class JPanelYBox extends JPanel {
	/** Create the panel and set BoxLayout.Y_AXIS */
	public JPanelYBox() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
	}
}

