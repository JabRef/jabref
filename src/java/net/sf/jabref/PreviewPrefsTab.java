package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PreviewPrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    HelpDialog helpDiag;
    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    JTextArea layout1 = new JTextArea("", 1, 1),
    layout2 = new JTextArea("", 1, 1);
    JButton def1 = new JButton(Globals.lang("Default")),
	def2 = new JButton(Globals.lang("Default"));
    JPanel p1 = new JPanel(),
	p2 = new JPanel();
    JScrollPane sp1 = new JScrollPane(layout1),
	sp2 = new JScrollPane(layout2);

    public PreviewPrefsTab(JabRefPreferences prefs, HelpDialog diag) {
	_prefs = prefs;
 	p1.setLayout(gbl);
 	p2.setLayout(gbl);
	p1.setBorder(BorderFactory.createTitledBorder
		     (BorderFactory.createEtchedBorder(),Globals.lang("Preview")+" 1"));
	p2.setBorder(BorderFactory.createTitledBorder
		     (BorderFactory.createEtchedBorder(),Globals.lang("Preview")+" 2"));
	setLayout(new GridLayout(2,1));
	JLabel lab;
	lab = new JLabel(Globals.lang("Preview")+" 1");
	con.anchor = GridBagConstraints.WEST;
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.BOTH;
	con.weightx = 1;
	con.weighty = 0;
	con.insets = new Insets(2,2,2,2);
	gbl.setConstraints(lab, con);
	//p1.add(lab);
	con.weighty = 1;
	gbl.setConstraints(sp1, con);
	p1.add(sp1);
	con.weighty = 0;
	con.fill = GridBagConstraints.NONE;
	gbl.setConstraints(def1, con);
	p1.add(def1);
	lab = new JLabel(Globals.lang("Preview")+" 2");
	gbl.setConstraints(lab, con);
	//p2.add(lab);
	con.weighty = 1;
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(sp2, con);
	p2.add(sp2);
	con.weighty = 0;
	con.fill = GridBagConstraints.NONE;
	gbl.setConstraints(def2, con);
	p2.add(def2);

	add(p1);
	add(p2);

	def1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String tmp = layout1.getText().replaceAll("\n", "__NEWLINE__");
		    _prefs.remove("preview0");
		    layout1.setText(_prefs.get("preview0").replaceAll("__NEWLINE__", "\n"));
		    _prefs.put("preview0", tmp);
		}
	    });
	def2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String tmp = layout2.getText().replaceAll("\n", "__NEWLINE__");
		    _prefs.remove("preview1");
		    layout2.setText(_prefs.get("preview1").replaceAll("__NEWLINE__", "\n"));
		    _prefs.put("preview1", tmp);
		}
	    });

    }

    public void setValues() {
	layout1.setText(_prefs.get("preview0").replaceAll("__NEWLINE__", "\n"));
	layout2.setText(_prefs.get("preview1").replaceAll("__NEWLINE__", "\n"));
    }

    public void storeSettings() {
	_prefs.put("preview0", layout1.getText().replaceAll("\n", "__NEWLINE__"));
	_prefs.put("preview1", layout2.getText().replaceAll("\n", "__NEWLINE__"));
    }

    public boolean readyToClose() {
	return true;
    }

}
