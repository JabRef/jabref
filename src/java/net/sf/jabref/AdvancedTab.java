package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;

public class AdvancedTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    HelpDialog helpDiag;
    JPanel pan = new JPanel(),
	lnf = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JLabel lab;
    JCheckBox useDefault;
    JTextField className;
    JButton def1 = new JButton(Globals.lang("Default")),
	def2 = new JButton(Globals.lang("Default"));
    JPanel p1 = new JPanel(),
	p2 = new JPanel();
    String oldLnf = "";
    boolean oldUseDef;

    public AdvancedTab(JabRefPreferences prefs, HelpDialog diag) {
	_prefs = prefs;

	oldUseDef = prefs.getBoolean("useDefaultLookAndFeel");
	oldLnf = prefs.get("lookAndFeel");
	useDefault = new JCheckBox(Globals.lang("Use other look and feel"),
				   !oldUseDef);

	className = new JTextField(oldLnf, 50);
	className.setEnabled(!oldUseDef);
	final JTextField clName = className;
	useDefault.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    clName.setEnabled(((JCheckBox)e.getSource()).isSelected());
		}
	    });

	setLayout(new BorderLayout());
 	lnf.setLayout(gbl);
	lnf.setBorder(BorderFactory.createTitledBorder
		     (BorderFactory.createEtchedBorder(),Globals.lang("Look and feel")));
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.weightx = 0;
	lab = new JLabel(Globals.lang("Default look and feel")+": "
			 +(Globals.ON_WIN ? GUIGlobals.windowsDefaultLookAndFeel :
			   GUIGlobals.linuxDefaultLookAndFeel));
	gbl.setConstraints(lab, con);
	lnf.add(lab);
	gbl.setConstraints(useDefault, con);
	lnf.add(useDefault);
	lab = new JLabel(Globals.lang("Class name")+":");
	con.insets = new Insets(0, 5, 2, 0);
	con.gridwidth = 1;
	gbl.setConstraints(lab, con);
	lnf.add(lab);
	con.weightx = 1;
	con.insets = new Insets(0, 5, 2, 25);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(className, con);
	lnf.add(className);
	con.insets = new Insets(0, 5, 2, 0);
	lab = new JLabel(Globals.lang("Note: You must specify the fully qualified class name for the look and feel,"));
	gbl.setConstraints(lab, con);
	lnf.add(lab);
	lab = new JLabel(Globals.lang("and the class must be available in your classpath next time you start JabRef."));
	gbl.setConstraints(lab, con);
	lnf.add(lab);

	add(lnf, BorderLayout.NORTH);

	init();
    }

    public void init() {

    }

    public void storeSettings() {
	_prefs.putBoolean("useDefaultLookAndFeel", !useDefault.isSelected());
	_prefs.put("lookAndFeel", className.getText());

	if ((useDefault.isSelected() == oldUseDef) ||
	    !oldLnf.equals(className.getText())) {
	    JOptionPane.showMessageDialog(null, Globals.lang("You have changed the look and feel setting. "
							     +"You must restart JabRef for this to come into effect."), Globals.lang("Changed look and feel settings"),
					  JOptionPane.WARNING_MESSAGE);
	}
    }

    public boolean readyToClose() {
	return true;
    }

}
