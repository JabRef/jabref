package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;

public class AdvancedTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    HelpDialog helpDiag;
    JPanel pan = new JPanel(),
	lnf = new JPanel();
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

	FormLayout layout = new FormLayout
	    ("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu",//, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
	     "");                	
	DefaultFormBuilder builder = new DefaultFormBuilder(layout);
	JPanel pan = new JPanel();
	builder.appendSeparator(Globals.lang("Look and feel"));
	JLabel lab = new JLabel(Globals.lang("Default look and feel")+": "
			 +(Globals.ON_WIN ? GUIGlobals.windowsDefaultLookAndFeel :
			   GUIGlobals.linuxDefaultLookAndFeel));
	builder.nextLine();
	builder.append(pan);
	builder.append(lab);
	builder.nextLine();
	builder.append(pan);
	builder.append(useDefault);
	builder.nextLine();
	builder.append(pan);
	JPanel pan2 = new JPanel();
	lab = new JLabel(Globals.lang("Class name")+":");
	pan2.add(lab);
	pan2.add(className);
	builder.append(pan2);
	builder.nextLine();
	builder.append(pan);
	lab = new JLabel(Globals.lang("Note: You must specify the fully qualified class name for the look and feel,"));
	builder.append(lab);
	builder.nextLine();
	builder.append(pan);
	lab = new JLabel(Globals.lang("and the class must be available in your classpath next time you start JabRef."));
	builder.append(lab);
	builder.nextLine();

	pan = builder.getPanel();
	pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	setLayout(new BorderLayout());
	add(pan, BorderLayout.CENTER);
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
