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

