
package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

public class GeneralTab extends JPanel implements PrefsTab {

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    private JCheckBox autoOpenForm, backup, openLast,
	defSource, editSource;
    private JTextField groupField = new JTextField(15);
    JabRefPreferences _prefs;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray());

    public GeneralTab(JabRefPreferences prefs) {
	_prefs = prefs;

	setLayout(gbl);
	con.weightx = 0;
	con.insets = new Insets(10, 10, 10, 10);
	con.fill = GridBagConstraints.HORIZONTAL;
	con.gridwidth = GridBagConstraints.REMAINDER;
	autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"),
				     _prefs.getBoolean("autoOpenForm"));
	openLast = new JCheckBox(Globals.lang
				 ("Open last edited databases at startup"),_prefs.getBoolean("openLastEdited"));
	backup = new JCheckBox(Globals.lang("Backup old file when saving"),
			       _prefs.getBoolean("backup"));
	defSource = new JCheckBox(Globals.lang("Show source by default"),
				  _prefs.getBoolean("defaultShowSource"));
	editSource = new JCheckBox(Globals.lang("Enable source editing"),
				   _prefs.getBoolean("enableSourceEditing"));
	JPanel general = new JPanel();
        groupField = new JTextField(_prefs.get("groupsDefaultField"), 15);
	general.setBorder(BorderFactory.createTitledBorder
			  (BorderFactory.createEtchedBorder(),
			   Globals.lang("Miscellaneous")));
	general.setLayout(gbl);

	gbl.setConstraints(openLast, con);
	general.add(openLast);

	gbl.setConstraints(backup, con);
	general.add(backup);

	gbl.setConstraints(autoOpenForm, con);
	general.add(autoOpenForm);

	gbl.setConstraints(defSource, con);
	general.add(defSource);

        // Grouping field
        con.gridwidth = 1;
        JLabel lab = new JLabel(Globals.lang("Default grouping field")+":");
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        gbl.setConstraints(lab, con);
        general.add(lab);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(groupField, con);
        general.add(groupField);

        // Language choice
        String oldLan = _prefs.get("language");
        int ilk = 0;
        for (Iterator i=GUIGlobals.LANGUAGES.keySet().iterator(); i.hasNext();) {
          if (GUIGlobals.LANGUAGES.get(i.next()).equals(oldLan)) {
            language.setSelectedIndex(ilk);
          }
          ilk++;
        }
        con.gridwidth = 1;
        lab = new JLabel(Globals.lang("Language")+":");
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        gbl.setConstraints(lab, con);
        general.add(lab);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(language, con);
        general.add(language);

        gbl.setConstraints(general, con);
        add(general);


    }

    public void storeSettings() {
	_prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
	_prefs.putBoolean("backup", backup.isSelected());
	_prefs.putBoolean("openLastEdited", openLast.isSelected());
	_prefs.putBoolean("defaultShowSource", defSource.isSelected());
	_prefs.putBoolean("enableSourceEditing", editSource.isSelected());
        _prefs.put("groupsDefaultField", groupField.getText().trim());

        if (!GUIGlobals.LANGUAGES.get(language.getSelectedItem()).equals(_prefs.get("language"))) {
          _prefs.put("language", GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString());
          Globals.setLanguage(GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString(), "");
          JOptionPane.showMessageDialog(null, Globals.lang("You have changed the language setting. "
              +"You must restart JabRef for this to come into effect."), Globals.lang("Changed language settings"),
                                        JOptionPane.WARNING_MESSAGE);
        }
    }

}
