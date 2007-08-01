package net.sf.jabref;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class TitleLabel extends JLabel {

    public TitleLabel(String txt, int orientation) {
	super(txt, orientation);
	setFont((java.awt.Font)UIManager.get("TitledBorder.font"));
	setForeground((java.awt.Color)UIManager.get("TitledBorder.titleColor"));
    }

    public TitleLabel(String txt) {
	this(txt, SwingConstants.CENTER);
    }
}
