package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdvancedTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    HelpDialog helpDiag;
    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    JTextArea layout1 = new JTextArea("", 1, 60),
	layout2 = new JTextArea("", 1, 60);
    JButton def1 = new JButton(Globals.lang("Default")),
	def2 = new JButton(Globals.lang("Default"));
    JPanel p1 = new JPanel(),
	p2 = new JPanel();
    JScrollPane sp1 = new JScrollPane(layout1),
	sp2 = new JScrollPane(layout2);

    public AdvancedTab(JabRefPreferences prefs, HelpDialog diag) {
	_prefs = prefs;
 	p1.setLayout(gbl);
	p1.setBorder(BorderFactory.createTitledBorder
		     (BorderFactory.createEtchedBorder(),Globals.lang("Preview")+" 1"));
	add(p1);

	init();
    }

    public void init() {

    }

    public void storeSettings() {

    }

    public boolean readyToClose() {
	return true;
    }

}
