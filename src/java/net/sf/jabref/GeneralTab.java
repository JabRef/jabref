
package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.io.File;

public class GeneralTab extends JPanel implements PrefsTab {

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    private JCheckBox autoOpenForm, backup, openLast,
		defSource, editSource,defSort, ctrlClick, disableOnMultiple;
    private JTextField groupField = new JTextField(15);
    private JTextField defOwnerField;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray());
    JTextField
	pdfDir, pdf, ps, html, lyx;

    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
	_prefs = prefs;
        _frame = frame;
 	setLayout(gbl);
	con.weightx = 0;
        //con.insets = new Insets(10, 10, 10, 10);
        con.insets = new Insets(5, 10, 5, 10);
	con.fill = GridBagConstraints.HORIZONTAL;

	autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"),
				     _prefs.getBoolean("autoOpenForm"));
	openLast = new JCheckBox(Globals.lang
				 ("Open last edited databases at startup"),_prefs.getBoolean("openLastEdited"));
	backup = new JCheckBox(Globals.lang("Backup old file when saving"),
			       _prefs.getBoolean("backup"));
	defSource = new JCheckBox(Globals.lang("Show BibTeX source by default"),
				  _prefs.getBoolean("defaultShowSource"));
	editSource = new JCheckBox(Globals.lang("Enable source editing"),
				   _prefs.getBoolean("enableSourceEditing"));
        defSort = new JCheckBox(Globals.lang("Sort Automatically"),
				  _prefs.getBoolean("defaultAutoSort"));
          ctrlClick = new JCheckBox(Globals.lang("Open right-click menu with Ctrl+left button"),
				  _prefs.getBoolean("ctrlClick"));
        disableOnMultiple = new JCheckBox(Globals.lang("Disable entry editor when multiple entries are selected"),
                                  _prefs.getBoolean("disableOnMultipleSelection"));
	JPanel general = new JPanel(),
	    external = new JPanel();
	defOwnerField = new JTextField(_prefs.get("defaultOwner"));
        groupField = new JTextField(_prefs.get("groupsDefaultField"), 15);
	general.setBorder(BorderFactory.createTitledBorder
			  (BorderFactory.createEtchedBorder(),
			   Globals.lang("Miscellaneous")));
	external.setBorder(BorderFactory.createTitledBorder
			  (BorderFactory.createEtchedBorder(),
			   Globals.lang("External programs")));
	general.setLayout(gbl);
	external.setLayout(gbl);

	con.gridwidth = 1;
	gbl.setConstraints(openLast, con);
	general.add(openLast);

	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(autoOpenForm, con);
	general.add(autoOpenForm);

        con.gridwidth = 1;
        gbl.setConstraints(backup, con);
	general.add(backup);

        con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(defSource, con);
	general.add(defSource);

        con.gridwidth = 1;
        gbl.setConstraints(disableOnMultiple, con);
        general.add(disableOnMultiple);

        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(ctrlClick, con);
        general.add(ctrlClick);

	//con.gridwidth = GridBagConstraints.REMAINDER;
	//gbl.setConstraints(defSort, con);
	//general.add(defSort);
	// Default owner
	con.gridwidth = 1;
	JLabel lab = new JLabel(Globals.lang("Default owner")+":");
        lab.setHorizontalAlignment(SwingConstants.LEFT);
	gbl.setConstraints(lab, con);
        general.add(lab);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(defOwnerField, con);
        general.add(defOwnerField);


        // Grouping field
        con.gridwidth = 1;
        lab = new JLabel(Globals.lang("Default grouping field")+":");
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


	// ------------------------------------------------------------
	// External programs panel.
	// ------------------------------------------------------------
        pdfDir = new JTextField(_prefs.get("pdfDirectory"), 30);
        pdf = new JTextField(_prefs.get("pdfviewer"), 30);
	ps = new JTextField(_prefs.get("psviewer"), 30);
	html = new JTextField(_prefs.get("htmlviewer"), 30);
	lyx = new JTextField(_prefs.get("lyxpipe"), 30);

        con.gridwidth = 1;
	con.weightx = 0;
       con.insets = new Insets(5, 10, 15, 10);
	con.fill = GridBagConstraints.HORIZONTAL;
        lab = new JLabel(Globals.lang("Main PDF directory")+":");
        gbl.setConstraints(lab, con);
        external.add(lab);
        con.weightx = 1;
        gbl.setConstraints(pdfDir, con);
        external.add(pdfDir);
        con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(pdfDir, true));
        gbl.setConstraints(browse, con);
        external.add(browse);
        con.gridwidth = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.insets = new Insets(5, 10, 5, 10);
        lab = new JLabel(Globals.lang("Path to PDF viewer")+":");
        gbl.setConstraints(lab, con);
        external.add(lab);
        con.weightx = 1;
        gbl.setConstraints(pdf, con);
        external.add(pdf);
        con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(pdf, false));
        gbl.setConstraints(browse, con);
        external.add(browse);
        con.gridwidth = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to PS viewer")+":");
	gbl.setConstraints(lab, con);
	external.add(lab);
	con.weightx = 1;
	gbl.setConstraints(ps, con);
	external.add(ps);
	con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(ps, false));
        gbl.setConstraints(browse, con);
        external.add(browse);
	con.gridwidth = 1;
	lab = new JLabel(Globals.lang("Path to HTML viewer")+":");
	gbl.setConstraints(lab, con);
	external.add(lab);
	con.weightx = 1;
	gbl.setConstraints(html, con);
	external.add(html);
        con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 0;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(html, false));
        gbl.setConstraints(browse, con);
        external.add(browse);
	con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to LyX pipe")+":");
	gbl.setConstraints(lab, con);
	external.add(lab);
        con.weightx = 1;
	gbl.setConstraints(lyx, con);
	external.add(lyx);
        con.weightx = 0;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(lyx, false));
        gbl.setConstraints(browse, con);
        external.add(browse);


	con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(general, con);
        add(general);

        gbl.setConstraints(external, con);
        add(external);
    }

  /**
   * Action used to produce a "Browse" button for one of the text fields.
   */
  class BrowseAction extends AbstractAction {
      JTextField comp;
      boolean dir;
      public BrowseAction(JTextField tc, boolean dir) {
        super(Globals.lang("Browse"));
        this.dir = dir;
        comp = tc;
      }
      public void actionPerformed(ActionEvent e) {
        String chosen = null;
        if (dir)
          chosen = Globals.getNewDir(_frame, _prefs, new File(comp.getText()), Globals.NONE,
                                     JFileChooser.OPEN_DIALOG, false);
        else
          chosen = Globals.getNewFile(_frame, _prefs, new File(comp.getText()), Globals.NONE,
                                      JFileChooser.OPEN_DIALOG, false);
        if (chosen != null) {
          File newFile = new File(chosen);
          comp.setText(newFile.getPath());
        }
      }
    }

    public void storeSettings() {
	_prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
	_prefs.putBoolean("backup", backup.isSelected());
	_prefs.putBoolean("openLastEdited", openLast.isSelected());
	_prefs.putBoolean("defaultShowSource", defSource.isSelected());
        _prefs.putBoolean("enableSourceEditing", editSource.isSelected());
        _prefs.putBoolean("disableOnMultipleSelection", disableOnMultiple.isSelected());

        _prefs.putBoolean("ctrlClick", ctrlClick.isSelected());
      //_prefs.putBoolean("defaultAutoSort", defSort.isSelected());
	_prefs.put("defaultOwner", defOwnerField.getText().trim());

        _prefs.put("groupsDefaultField", groupField.getText().trim());

	// We should maybe do some checking on the validity of the contents?
        _prefs.put("pdfDirectory", pdfDir.getText());
        _prefs.put("pdfviewer", pdf.getText());
	_prefs.put("psviewer", ps.getText());
	_prefs.put("htmlviewer", html.getText());
	_prefs.put("lyxpipe", lyx.getText());


        if (!GUIGlobals.LANGUAGES.get(language.getSelectedItem()).equals(_prefs.get("language"))) {
          _prefs.put("language", GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString());
          Globals.setLanguage(GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString(), "");
          JOptionPane.showMessageDialog(null, Globals.lang("You have changed the language setting. "
              +"You must restart JabRef for this to come into effect."), Globals.lang("Changed language settings"),
                                        JOptionPane.WARNING_MESSAGE);
        }
    }

}
