package net.sf.jabref;

import javax.swing.*;
import java.awt.*;

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
