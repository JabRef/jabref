
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
	allowEditing, preserveFormatting;
    private JTextField groupField = new JTextField(15);
    private JTextField defOwnerField, fontSize;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray()),
        encodings = new JComboBox(Globals.ENCODINGS);
    private HelpAction ownerHelp, pdfHelp;
    private int oldMenuFontSize;

    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
	_prefs = prefs;
        _frame = frame;
 	setLayout(new BorderLayout());

	autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"));
	openLast = new JCheckBox(Globals.lang("Open last edited databases at startup"));
        allowEditing = new JCheckBox(Globals.lang("Allow editing in table cells"));
	backup = new JCheckBox(Globals.lang("Backup old file when saving"));
	defSource = new JCheckBox(Globals.lang("Show BibTeX source by default"));
	editSource = new JCheckBox(Globals.lang("Enable source editing"));
        defSort = new JCheckBox(Globals.lang("Sort Automatically"));
	ctrlClick = new JCheckBox(Globals.lang("Open right-click menu with Ctrl+left button"));
        disableOnMultiple = new JCheckBox(Globals.lang("Disable entry editor when multiple entries are selected"));
	useOwner = new JCheckBox(Globals.lang("Mark new entries with owner name")+":");
	keyWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when a duplicate BibTeX key is entered"));
	confirmDelete = new JCheckBox(Globals.lang("Show confirmation dialog when deleting entries"));
	saveInStandardOrder = new JCheckBox(Globals.lang("Always save database ordered by author name"));
        preserveFormatting = new JCheckBox(Globals.lang("Preserve formatting of non-BibTeX fields"));
	JPanel general = new JPanel();
	defOwnerField = new JTextField();
        groupField = new JTextField(15);
	ownerHelp = new HelpAction(frame.helpDiag, GUIGlobals.ownerHelp,
				   "Help", GUIGlobals.helpSmallIconFile);

	 // Font sizes:
	 fontSize = new JTextField();	 	
        
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
        builder.append(pan); builder.append(preserveFormatting); builder.nextLine();
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
	lab = new JLabel(Globals.lang("Menu and label font size")+":");
	builder2.nextLine();
	builder2.append(lab); 
	builder2.append(fontSize); 
	builder.append(pan);
	builder.append(builder2.getPanel());
	builder.nextLine();
	//builder.appendSeparator();

	pan = builder.getPanel();
	pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	add(pan, BorderLayout.CENTER);

    }

    public void setValues() {
	autoOpenForm.setSelected(_prefs.getBoolean("autoOpenForm"));
	openLast.setSelected(_prefs.getBoolean("openLastEdited"));
	allowEditing.setSelected( _prefs.getBoolean("allowTableEditing"));
	backup.setSelected(_prefs.getBoolean("backup"));
	defSource.setSelected( _prefs.getBoolean("defaultShowSource"));
	editSource.setSelected(_prefs.getBoolean("enableSourceEditing"));
	defSort.setSelected(_prefs.getBoolean("defaultAutoSort"));
	ctrlClick.setSelected(_prefs.getBoolean("ctrlClick"));
	disableOnMultiple.setSelected(_prefs.getBoolean("disableOnMultipleSelection"));
	useOwner.setSelected(_prefs.getBoolean("useOwner"));
	keyWarningDialog.setSelected(_prefs.getBoolean("dialogWarningForDuplicateKey"));
	confirmDelete.setSelected(_prefs.getBoolean("confirmDelete"));
	saveInStandardOrder.setSelected(_prefs.getBoolean("saveInStandardOrder"));
	preserveFormatting.setSelected(_prefs.getBoolean("preserveFieldFormatting"));
        defOwnerField.setText(_prefs.get("defaultOwner"));
	groupField.setText(_prefs.get("groupsDefaultField"));
        
	String enc = _prefs.get("defaultEncoding");
        outer: for (int i=0; i<Globals.ENCODINGS.length; i++) {
	    if (Globals.ENCODINGS[i].equalsIgnoreCase(enc)) {
		encodings.setSelectedIndex(i);
		break outer;
	    }
	}
	fontSize.setText(""+_prefs.getInt("menuFontSize"));
	oldMenuFontSize = _prefs.getInt("menuFontSize");
	String oldLan = _prefs.get("language");

	// Language choice
        int ilk = 0;
        for (Iterator i=GUIGlobals.LANGUAGES.keySet().iterator(); i.hasNext();) {
          if (GUIGlobals.LANGUAGES.get(i.next()).equals(oldLan)) {
            language.setSelectedIndex(ilk);
          }
          ilk++;
        }

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
        _prefs.putBoolean("preserveFieldFormatting", preserveFormatting.isSelected());
      //_prefs.putBoolean("defaultAutoSort", defSorrrt.isSelected());
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

	try {
	    int size = Integer.parseInt(fontSize.getText());	    
	    if (size != oldMenuFontSize) {
		_prefs.putInt("menuFontSize", size);
		JOptionPane.showMessageDialog(null, Globals.lang("You have changed the menu and label font size. "
								 +"You must restart JabRef for this to come into effect."), Globals.lang("Changed font settings"),
					      JOptionPane.WARNING_MESSAGE);
	    }
	    
	} catch (NumberFormatException ex) {
	    ex.printStackTrace();
	}
    }

    public boolean readyToClose() {
	try {
	    int size = Integer.parseInt(fontSize.getText());	    
	    return true; // Ok, the number was legal.
	} catch (NumberFormatException ex) {
	    JOptionPane.showMessageDialog
		(null, Globals.lang("You must enter an integer value in the text field for")+" '"+
		 Globals.lang("Menu and label font size")+"'", Globals.lang("Changed font settings"),
		 JOptionPane.ERROR_MESSAGE);
	    return false;
	}
    }

}
