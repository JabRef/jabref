
package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.io.File;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;

public class GeneralTab extends JPanel implements PrefsTab {

    private JCheckBox autoOpenForm, backup, openLast,
	defSource, editSource,defSort, ctrlClick, disableOnMultiple,
	useOwner, keyWarningDialog, confirmDelete, saveInStandardOrder,
	allowEditing;
    private JTextField groupField = new JTextField(15);
    private JTextField defOwnerField;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray()),
        encodings = new JComboBox(Globals.ENCODINGS);
    private HelpAction ownerHelp, pdfHelp;


    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
	_prefs = prefs;
        _frame = frame;
 	setLayout(new BorderLayout());

	autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"),
				     _prefs.getBoolean("autoOpenForm"));
	openLast = new JCheckBox(Globals.lang
				 ("Open last edited databases at startup"),_prefs.getBoolean("openLastEdited"));
        allowEditing = new JCheckBox(Globals.lang("Allow editing in table cells"), _prefs.getBoolean("allowTableEditing"));
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
useOwner = new JCheckBox(Globals.lang("Mark new entries with owner name")+":",
                         _prefs.getBoolean("useOwner"));
keyWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when a duplicate BibTeX key is entered"),
                                 _prefs.getBoolean("dialogWarningForDuplicateKey"));
confirmDelete = new JCheckBox(Globals.lang("Show confirmation dialog when deleting entries"),
                              _prefs.getBoolean("confirmDelete"));
saveInStandardOrder = new JCheckBox(Globals.lang("Always save database ordered by author name"),
				    _prefs.getBoolean("saveInStandardOrder"));
JPanel general = new JPanel();
defOwnerField = new JTextField(_prefs.get("defaultOwner"));
        groupField = new JTextField(_prefs.get("groupsDefaultField"), 15);
       ownerHelp = new HelpAction(frame.helpDiag, GUIGlobals.ownerHelp,
                                  "Help", GUIGlobals.helpSmallIconFile);

         String enc = prefs.get("defaultEncoding");
         outer: for (int i=0; i<Globals.ENCODINGS.length; i++) {
           if (Globals.ENCODINGS[i].equalsIgnoreCase(enc)) {
             encodings.setSelectedIndex(i);
             break outer;
           }
         }

	// Language choice
        String oldLan = _prefs.get("language");
        int ilk = 0;
        for (Iterator i=GUIGlobals.LANGUAGES.keySet().iterator(); i.hasNext();) {
          if (GUIGlobals.LANGUAGES.get(i.next()).equals(oldLan)) {
            language.setSelectedIndex(ilk);
          }
          ilk++;
        }

	FormLayout layout = new FormLayout
	    ("1dlu, 8dlu, left:pref, 4dlu, fill:60dlu, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
	     "");                	
	DefaultFormBuilder builder = new DefaultFormBuilder(layout);
	JPanel pan = new JPanel();
	builder.appendSeparator(Globals.lang("File"));
	builder.nextLine();
	builder.append(pan); builder.append(openLast); builder.nextLine();
	builder.append(pan); builder.append(backup); builder.nextLine();
	builder.append(pan); builder.append(saveInStandardOrder); builder.nextLine();
	//builder.appendSeparator(Globals.lang("Miscellaneous"));
	//builder.nextLine();
	builder.appendSeparator(Globals.lang("Entry editor"));
	builder.nextLine();
	builder.append(pan); builder.append(autoOpenForm); builder.nextLine();
	builder.append(pan); builder.append(defSource); builder.nextLine();
	builder.append(pan); builder.append(disableOnMultiple); builder.nextLine();
	builder.appendSeparator(Globals.lang("Miscellaneous"));
	builder.append(pan); builder.append(allowEditing); builder.nextLine();
	builder.append(pan); builder.append(ctrlClick); builder.nextLine();
	builder.append(pan); builder.append(confirmDelete); builder.nextLine();
	builder.append(pan); builder.append(keyWarningDialog); builder.nextLine();
	// Create a new panel with its own FormLayout for the last items:
	FormLayout layout2 = new FormLayout
	    ("left:pref, 8dlu, fill:60dlu, 4dlu, fill:pref", "");                
	DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
	builder2.append(useOwner); builder2.append(defOwnerField);
	JButton hlp = new JButton(ownerHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24,24));
	builder2.append(hlp); builder2.nextLine();
	JLabel lab = new JLabel(Globals.lang("Default grouping field")+":");
	builder2.append(lab);
	builder2.append(groupField); builder2.nextLine();
        lab = new JLabel(Globals.lang("Language")+":");
	builder2.append(lab); 
	builder2.append(language); builder2.nextLine();
	lab = new JLabel(Globals.lang("Default encoding")+":");
	builder2.append(lab); 
	builder2.append(encodings);

	builder.append(pan);
	builder.append(builder2.getPanel());
	builder.nextLine();
	//builder.appendSeparator();

	pan = builder.getPanel();
	pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	add(pan, BorderLayout.CENTER);

    }

    public void storeSettings() {
	_prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
	_prefs.putBoolean("backup", backup.isSelected());
	_prefs.putBoolean("openLastEdited", openLast.isSelected());
	_prefs.putBoolean("defaultShowSource", defSource.isSelected());
        _prefs.putBoolean("enableSourceEditing", editSource.isSelected());
        _prefs.putBoolean("disableOnMultipleSelection", disableOnMultiple.isSelected());
        _prefs.putBoolean("useOwner", useOwner.isSelected());
        _prefs.putBoolean("dialogWarningForDuplicateKey", keyWarningDialog.isSelected());
        _prefs.putBoolean("confirmDelete", confirmDelete.isSelected());
        _prefs.putBoolean("saveInStandardOrder", saveInStandardOrder.isSelected());
        _prefs.putBoolean("allowTableEditing", allowEditing.isSelected());
        _prefs.putBoolean("ctrlClick", ctrlClick.isSelected());
      //_prefs.putBoolean("defaultAutoSort", defSort.isSelected());
	_prefs.put("defaultOwner", defOwnerField.getText().trim());

        _prefs.put("groupsDefaultField", groupField.getText().trim());
        _prefs.put("defaultEncoding", (String)encodings.getSelectedItem());

        if (!GUIGlobals.LANGUAGES.get(language.getSelectedItem()).equals(_prefs.get("language"))) {
          _prefs.put("language", GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString());
          Globals.setLanguage(GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString(), "");
          JOptionPane.showMessageDialog(null, Globals.lang("You have changed the language setting. "
              +"You must restart JabRef for this to come into effect."), Globals.lang("Changed language settings"),
                                        JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean readyToClose() {
	return true;
    }

}
