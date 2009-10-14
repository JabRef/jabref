package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class GeneralTab extends JPanel implements PrefsTab {

    private JCheckBox
    defSort, ctrlClick, useOwner, overwriteOwner,
    keyDuplicateWarningDialog, keyEmptyWarningDialog, enforceLegalKeys,
    confirmDelete, allowEditing, memoryStick, useImportInspector,
    useImportInspectorForSingle, inspectionWarnDupli, useTimeStamp, overwriteTimeStamp,
    markImportedEntries, unmarkAllEntriesBeforeImporting;

    private JTextField defOwnerField, timeStampFormat, timeStampField;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray()),
    encodings = new JComboBox(Globals.ENCODINGS);
    private HelpAction ownerHelp, timeStampHelp;

    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        _frame = frame;
        setLayout(new BorderLayout());


        allowEditing = new JCheckBox(Globals.lang("Allow editing in table cells"));

        memoryStick = new JCheckBox(Globals.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        defSort = new JCheckBox(Globals.lang("Sort Automatically"));
        ctrlClick = new JCheckBox(Globals.lang("Open right-click menu with Ctrl+left button"));
        useOwner = new JCheckBox(Globals.lang("Mark new entries with owner name") + ":");
        useTimeStamp = new JCheckBox(Globals.lang("Mark new entries with addition date") + ". "
            +Globals.lang("Date format")+ ":");
        overwriteOwner = new JCheckBox(Globals.lang("Overwrite"));
        overwriteTimeStamp = new JCheckBox(Globals.lang("Overwrite"));
        overwriteOwner.setToolTipText(Globals.lang("If a pasted or imported entry already has "
            +"the field set, overwrite."));
        overwriteTimeStamp.setToolTipText(Globals.lang("If a pasted or imported entry already has "
            +"the field set, overwrite."));
        keyDuplicateWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when a duplicate BibTeX key is entered"));
        keyEmptyWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when an empty BibTeX key is entered")); // JZTODO lyrics
        enforceLegalKeys = new JCheckBox(Globals.lang("Enforce legal characters in BibTeX keys"));
        confirmDelete = new JCheckBox(Globals.lang("Show confirmation dialog when deleting entries"));

        useImportInspector = new JCheckBox(Globals.lang("Display imported entries in an inspection window before they are added."));
        useImportInspectorForSingle = new JCheckBox(Globals.lang("Use inspection window also when a single entry is imported."));
        markImportedEntries = new JCheckBox(Globals.lang("Mark entries imported into an existing database"));
        unmarkAllEntriesBeforeImporting = new JCheckBox(Globals.lang("Unmark all entries before importing new entries into an existing database"));
        defOwnerField = new JTextField();
        timeStampFormat = new JTextField();
        timeStampField = new JTextField();
        ownerHelp = new HelpAction(frame.helpDiag, GUIGlobals.ownerHelp,
                "Help", GUIGlobals.getIconUrl("helpSmall"));
        timeStampHelp = new HelpAction(frame.helpDiag, GUIGlobals.timeStampHelp, "Help",
                GUIGlobals.getIconUrl("helpSmall"));
        inspectionWarnDupli = new JCheckBox(Globals.lang("Warn about unresolved duplicates when closing inspection window"));

        Insets marg = new Insets(0,12,3,0);
        useImportInspectorForSingle.setMargin(marg);
        inspectionWarnDupli.setMargin(marg);

        
        // We need a listener on useImportInspector to enable and disable the
        // import inspector related choices;
        useImportInspector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
                inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
            }
        });

        FormLayout layout = new FormLayout
                ("8dlu, 1dlu, left:170dlu, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, left:pref, 4dlu, left:pref, 4dlu, left:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);



        builder.appendSeparator(Globals.lang("General"));
        builder.nextLine();
        builder.append(useImportInspector, 13);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(useImportInspectorForSingle, 11);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(inspectionWarnDupli, 11);
        builder.nextLine();
        builder.append(ctrlClick, 13);
        builder.nextLine();
        builder.append(confirmDelete, 13);
        builder.nextLine();
        builder.append(keyDuplicateWarningDialog, 13);
        builder.nextLine();
        builder.append(keyEmptyWarningDialog, 13);
        builder.nextLine();
        builder.append(enforceLegalKeys, 13);
        builder.nextLine();
        builder.append(memoryStick, 13);
        
        // Create a new panel with its own FormLayout for the last items:
        builder.append(useOwner, 3);
        builder.append(defOwnerField);
        builder.append(overwriteOwner);
        builder.append(new JPanel(), 3);
        
        JButton hlp = new JButton(ownerHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder.append(hlp);
        builder.nextLine();

        builder.append(useTimeStamp, 3);
        builder.append(timeStampFormat);
        builder.append(overwriteTimeStamp);
        builder.append(Globals.lang("Field name")+":");
        builder.append(timeStampField);

        hlp = new JButton(timeStampHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder.append(hlp);
        builder.nextLine();

        builder.append(markImportedEntries, 13);
        builder.nextLine();
        builder.append(unmarkAllEntriesBeforeImporting, 13);
        builder.nextLine();
        JLabel lab;
        lab = new JLabel(Globals.lang("Language") + ":");
        builder.append(lab, 3);
        builder.append(language);
        builder.nextLine();
        lab = new JLabel(Globals.lang("Default encoding") + ":");
        builder.append(lab, 3);
        builder.append(encodings);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    public void setValues() {
        allowEditing.setSelected(_prefs.getBoolean("allowTableEditing"));
        defSort.setSelected(_prefs.getBoolean("defaultAutoSort"));
        ctrlClick.setSelected(_prefs.getBoolean("ctrlClick"));
        useOwner.setSelected(_prefs.getBoolean("useOwner"));
        overwriteOwner.setSelected(_prefs.getBoolean("overwriteOwner"));
        useTimeStamp.setSelected(_prefs.getBoolean("useTimeStamp"));
        overwriteTimeStamp.setSelected(_prefs.getBoolean("overwriteTimeStamp"));
        keyDuplicateWarningDialog.setSelected(_prefs.getBoolean("dialogWarningForDuplicateKey"));
        keyEmptyWarningDialog.setSelected(_prefs.getBoolean("dialogWarningForEmptyKey"));
        enforceLegalKeys.setSelected(_prefs.getBoolean("enforceLegalBibtexKey"));
        memoryStick.setSelected(_prefs.getBoolean("memoryStickMode"));
        confirmDelete.setSelected(_prefs.getBoolean("confirmDelete"));
        defOwnerField.setText(_prefs.get("defaultOwner"));
        timeStampFormat.setText(_prefs.get("timeStampFormat"));
        timeStampField.setText(_prefs.get("timeStampField"));
        useImportInspector.setSelected(_prefs.getBoolean("useImportInspectionDialog"));
        useImportInspectorForSingle.setSelected(_prefs.getBoolean("useImportInspectionDialogForSingle"));
        inspectionWarnDupli.setSelected(_prefs.getBoolean("warnAboutDuplicatesInInspection"));
        useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
        inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
        markImportedEntries.setSelected(_prefs.getBoolean("markImportedEntries"));
        unmarkAllEntriesBeforeImporting.setSelected(_prefs.getBoolean("unmarkAllEntriesBeforeImporting"));

        String enc = _prefs.get("defaultEncoding");
        for (int i = 0; i < Globals.ENCODINGS.length; i++) {
            if (Globals.ENCODINGS[i].equalsIgnoreCase(enc)) {
                encodings.setSelectedIndex(i);
                break;
            }
        }
        String oldLan = _prefs.get("language");

        // Language choice
        int ilk = 0;
        for (String lan : GUIGlobals.LANGUAGES.values()){
            if (lan.equals(oldLan)) {
                language.setSelectedIndex(ilk);
            }
            ilk++;
        }

    }

    public void storeSettings() {
        _prefs.putBoolean("useOwner", useOwner.isSelected());
        _prefs.putBoolean("overwriteOwner", overwriteOwner.isSelected());
        _prefs.putBoolean("useTimeStamp", useTimeStamp.isSelected());
        _prefs.putBoolean("overwriteTimeStamp", overwriteTimeStamp.isSelected());
        _prefs.putBoolean("dialogWarningForDuplicateKey", keyDuplicateWarningDialog.isSelected());
        _prefs.putBoolean("dialogWarningForEmptyKey", keyEmptyWarningDialog.isSelected());
        _prefs.putBoolean("enforceLegalBibtexKey", enforceLegalKeys.isSelected());
        if (_prefs.getBoolean("memoryStickMode") && !memoryStick.isSelected()){
            JOptionPane.showMessageDialog(null, Globals.lang("To disable the memory stick mode" +
            		" rename or remove the jabref.xml file in the same folder as JabRef."),
            		Globals.lang("Memory Stick Mode"),
            		JOptionPane.INFORMATION_MESSAGE);
        }
        _prefs.putBoolean("memoryStickMode", memoryStick.isSelected());
        _prefs.putBoolean("confirmDelete", confirmDelete.isSelected());
        _prefs.putBoolean("allowTableEditing", allowEditing.isSelected());
        _prefs.putBoolean("ctrlClick", ctrlClick.isSelected());
        //_prefs.putBoolean("preserveFieldFormatting", preserveFormatting.isSelected());
        _prefs.putBoolean("useImportInspectionDialog", useImportInspector.isSelected());
        _prefs.putBoolean("useImportInspectionDialogForSingle", useImportInspectorForSingle.isSelected());
        _prefs.putBoolean("warnAboutDuplicatesInInspection", inspectionWarnDupli.isSelected());
        //_prefs.putBoolean("defaultAutoSort", defSorrrt.isSelected());
        String owner = defOwnerField.getText().trim();
        _prefs.put("defaultOwner", owner);
        _prefs.WRAPPED_USERNAME = "["+owner+"]";
        _prefs.put("timeStampFormat", timeStampFormat.getText().trim());
        _prefs.put("timeStampField", timeStampField.getText().trim());
        _prefs.put("defaultEncoding", (String) encodings.getSelectedItem());
        _prefs.putBoolean("markImportedEntries", markImportedEntries.isSelected());
        _prefs.putBoolean("unmarkAllEntriesBeforeImporting", unmarkAllEntriesBeforeImporting.isSelected());


        if (!GUIGlobals.LANGUAGES.get(language.getSelectedItem()).equals(_prefs.get("language"))) {
            _prefs.put("language", GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString());
            Globals.setLanguage(GUIGlobals.LANGUAGES.get(language.getSelectedItem()).toString(), "");
            JOptionPane.showMessageDialog(null, Globals.lang("You have changed the language setting. "
                    + "You must restart JabRef for this to come into effect."), Globals.lang("Changed language settings"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean readyToClose() {
        try {
            // Test if date format is legal:
            new SimpleDateFormat(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("The chosen date format for new entries is not valid"),
                            Globals.lang("Invalid date format"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

	public String getTabName() {
		return Globals.lang("General");
	}
}
