package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.builder.*;

public class GeneralTab extends JPanel implements PrefsTab {

    private JCheckBox autoOpenForm, backup, openLast, showSource,
    defSource, editSource, defSort, ctrlClick, disableOnMultiple,
    useOwner, keyDuplicateWarningDialog, keyEmptyWarningDialog, autoDoubleBraces,
    confirmDelete, saveInStandardOrder, allowEditing, /*preserveFormatting, */useImportInspector,
    useImportInspectorForSingle, inspectionWarnDupli, useTimeStamp;
    private JTextField defOwnerField, timeStampFormat, timeStampField,
            bracesAroundCapitalsFields, nonWrappableFields;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray()),
    encodings = new JComboBox(Globals.ENCODINGS);
    private HelpAction ownerHelp, timeStampHelp;

    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        _frame = frame;
        setLayout(new BorderLayout());

        autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"));
        openLast = new JCheckBox(Globals.lang("Open last edited databases at startup"));
        allowEditing = new JCheckBox(Globals.lang("Allow editing in table cells"));
        backup = new JCheckBox(Globals.lang("Backup old file when saving"));
        defSource = new JCheckBox(Globals.lang("Show BibTeX source by default"));
        showSource = new JCheckBox(Globals.lang("Show BibTeX source panel"));
        editSource = new JCheckBox(Globals.lang("Enable source editing"));
        defSort = new JCheckBox(Globals.lang("Sort Automatically"));
        ctrlClick = new JCheckBox(Globals.lang("Open right-click menu with Ctrl+left button"));
        disableOnMultiple = new JCheckBox(Globals.lang("Disable entry editor when multiple entries are selected"));
        useOwner = new JCheckBox(Globals.lang("Mark new entries with owner name") + ":");
        useTimeStamp = new JCheckBox(Globals.lang("Mark new entries with addition date") + ". "
            +Globals.lang("Date format")+ ":");
        keyDuplicateWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when a duplicate BibTeX key is entered"));
        keyEmptyWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when an empty BibTeX key is entered")); // JZTODO lyrics
        confirmDelete = new JCheckBox(Globals.lang("Show confirmation dialog when deleting entries"));
        saveInStandardOrder = new JCheckBox(Globals.lang("Always save database ordered by author name"));
        autoDoubleBraces = new JCheckBox(
                //+ Globals.lang("Store fields with double braces, and remove extra braces when loading.<BR>"
                //+ "Double braces signal that BibTeX should preserve character case.") + "</HTML>");
                Globals.lang("Remove double braces around BibTeX fields when loading."));
        useImportInspector = new JCheckBox(Globals.lang("Display imported entries in an inspection window before they are added."));
        useImportInspectorForSingle = new JCheckBox(Globals.lang("Use inspection window also when a single entry is imported.")); 
        JPanel general = new JPanel();
        defOwnerField = new JTextField();
        timeStampFormat = new JTextField();
        timeStampField = new JTextField();
        ownerHelp = new HelpAction(frame.helpDiag, GUIGlobals.ownerHelp,
                "Help", GUIGlobals.helpSmallIconFile);
        timeStampHelp = new HelpAction(frame.helpDiag, GUIGlobals.timeStampHelp, "Help",
                GUIGlobals.helpSmallIconFile);
        inspectionWarnDupli = new JCheckBox(Globals.lang("Warn about unresolved duplicates when closing inspection window"));

        Insets marg = new Insets(0,12,3,0);
        useImportInspectorForSingle.setMargin(marg);
        editSource.setMargin(marg);
        defSource.setMargin(marg);
        inspectionWarnDupli.setMargin(marg);
        bracesAroundCapitalsFields = new JTextField(30);
        nonWrappableFields = new JTextField(30);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        showSource.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                defSource.setEnabled(showSource.isSelected());
                editSource.setEnabled(showSource.isSelected());
            }
        }
        );
        // We need a listener on useImportInspector to enable and disable the
        // import inspector related choices;
        useImportInspector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
                inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
            }
        }
        );

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                        "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();
        builder.appendSeparator(Globals.lang("File"));
        builder.nextLine();
        builder.append(pan);
        builder.append(openLast);
        builder.nextLine();
        builder.append(pan);
        builder.append(backup);
        builder.nextLine();
        builder.append(pan);
        builder.append(saveInStandardOrder);
        builder.nextLine();
        builder.append(pan);
        //builder.append(preserveFormatting);
        //builder.nextLine();
        //builder.append(pan);
        builder.append(autoDoubleBraces);
        builder.nextLine();
        JLabel label = new JLabel(Globals.lang("Store the following fields with braces around capital letters")+":");
        DefaultFormBuilder builder3 = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:pref",""));
        //panel.setLayout
        builder3.append(label);
        builder3.append(bracesAroundCapitalsFields);
        //
        //builder.append(panel);
        //builder.nextLine();
        label = new JLabel(Globals.lang("Do not wrap the following fields when saving")+":");
        builder3.append(label);
        builder3.append(nonWrappableFields);
        builder.append(pan);
        builder.append(builder3.getPanel());
        //builder.append(panel);
        builder.nextLine();

        //builder.appendSeparator(Globals.lang("Miscellaneous"));
        //builder.nextLine();
        builder.appendSeparator(Globals.lang("Entry editor"));
        builder.nextLine();
        builder.append(pan);
        builder.append(autoOpenForm);
        builder.nextLine();
        builder.append(pan);
        builder.append(disableOnMultiple);
        builder.nextLine();
        builder.append(pan);
        builder.append(showSource);
        builder.nextLine();
        builder.append(pan);
        builder.append(defSource);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Miscellaneous"));
        builder.append(pan);
        builder.append(useImportInspector);
        builder.nextLine();
        builder.append(pan);
        builder.append(useImportInspector);
        builder.nextLine();
        builder.append(pan);
        builder.append(useImportInspectorForSingle);
        builder.nextLine();
        builder.append(pan);
        builder.append(inspectionWarnDupli);
        builder.nextLine();
        builder.append(pan);
        builder.append(ctrlClick);
        builder.nextLine();
        builder.append(pan);
        builder.append(confirmDelete);
        builder.nextLine();
        builder.append(pan);
        builder.append(keyDuplicateWarningDialog);
        builder.nextLine();
        builder.append(pan);
        builder.append(keyEmptyWarningDialog);
        builder.nextLine();
        // Create a new panel with its own FormLayout for the last items:
        FormLayout layout2 = new FormLayout
                ("left:pref, 8dlu, fill:60dlu, 4dlu, left:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        builder2.append(useOwner);
        builder2.append(defOwnerField);
        JButton hlp = new JButton(ownerHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder2.append(hlp);
        builder2.nextLine();
        builder2.append(useTimeStamp);
        builder2.append(timeStampFormat);
        builder2.append(Globals.lang("Field name")+":");
        builder2.append(timeStampField);
        hlp = new JButton(timeStampHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder2.append(hlp);
        builder2.nextLine();
        JLabel lab;
        lab = new JLabel(Globals.lang("Language") + ":");
        builder2.append(lab);
        builder2.append(language);
        builder2.nextLine();
        lab = new JLabel(Globals.lang("Default encoding") + ":");
        builder2.append(lab);
        builder2.append(encodings);
        builder.append(pan);
        builder.append(builder2.getPanel());
        builder.nextLine();
        //builder.appendSeparator();

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    public void setValues() {
        autoOpenForm.setSelected(_prefs.getBoolean("autoOpenForm"));
        openLast.setSelected(_prefs.getBoolean("openLastEdited"));
        allowEditing.setSelected(_prefs.getBoolean("allowTableEditing"));
        backup.setSelected(_prefs.getBoolean("backup"));
        defSource.setSelected(_prefs.getBoolean("defaultShowSource"));
        showSource.setSelected(_prefs.getBoolean("showSource"));
        editSource.setSelected(_prefs.getBoolean("enableSourceEditing"));
        defSort.setSelected(_prefs.getBoolean("defaultAutoSort"));
        ctrlClick.setSelected(_prefs.getBoolean("ctrlClick"));
        disableOnMultiple.setSelected(_prefs.getBoolean("disableOnMultipleSelection"));
        useOwner.setSelected(_prefs.getBoolean("useOwner"));
        useTimeStamp.setSelected(_prefs.getBoolean("useTimeStamp"));
        keyDuplicateWarningDialog.setSelected(_prefs.getBoolean("dialogWarningForDuplicateKey"));
        keyEmptyWarningDialog.setSelected(_prefs.getBoolean("dialogWarningForEmptyKey"));
        confirmDelete.setSelected(_prefs.getBoolean("confirmDelete"));
        saveInStandardOrder.setSelected(_prefs.getBoolean("saveInStandardOrder"));
        //preserveFormatting.setSelected(_prefs.getBoolean("preserveFieldFormatting"));
        autoDoubleBraces.setSelected(_prefs.getBoolean("autoDoubleBraces"));
        defOwnerField.setText(_prefs.get("defaultOwner"));
        timeStampFormat.setText(_prefs.get("timeStampFormat"));
        timeStampField.setText(_prefs.get("timeStampField"));
        useImportInspector.setSelected(_prefs.getBoolean("useImportInspectionDialog"));
        useImportInspectorForSingle.setSelected(_prefs.getBoolean("useImportInspectionDialogForSingle"));
        inspectionWarnDupli.setSelected(_prefs.getBoolean("warnAboutDuplicatesInInspection"));
        // Two choices only make sense when the source panel is visible:
        defSource.setEnabled(showSource.isSelected());
        editSource.setEnabled(showSource.isSelected());
        useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
        inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
        bracesAroundCapitalsFields.setText(_prefs.get("putBracesAroundCapitals"));
        nonWrappableFields.setText(_prefs.get("nonWrappableFields"));

        String enc = _prefs.get("defaultEncoding");
        outer: for (int i = 0; i < Globals.ENCODINGS.length; i++) {
            if (Globals.ENCODINGS[i].equalsIgnoreCase(enc)) {
                encodings.setSelectedIndex(i);
                break outer;
            }
        }
        String oldLan = _prefs.get("language");

        // Language choice
        int ilk = 0;
        for (Iterator i = GUIGlobals.LANGUAGES.keySet().iterator(); i.hasNext();) {
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
        _prefs.putBoolean("useTimeStamp", useTimeStamp.isSelected());
        _prefs.putBoolean("dialogWarningForDuplicateKey", keyDuplicateWarningDialog.isSelected());
        _prefs.putBoolean("dialogWarningForEmptyKey", keyEmptyWarningDialog.isSelected());
        _prefs.putBoolean("confirmDelete", confirmDelete.isSelected());
        _prefs.putBoolean("saveInStandardOrder", saveInStandardOrder.isSelected());
        _prefs.putBoolean("allowTableEditing", allowEditing.isSelected());
        _prefs.putBoolean("ctrlClick", ctrlClick.isSelected());
        //_prefs.putBoolean("preserveFieldFormatting", preserveFormatting.isSelected());
        _prefs.putBoolean("autoDoubleBraces", autoDoubleBraces.isSelected());
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
        boolean updateSpecialFields = false;
        if (!bracesAroundCapitalsFields.getText().trim().equals(_prefs.get("putBracesAroundCapitals"))) {
            _prefs.put("putBracesAroundCapitals", bracesAroundCapitalsFields.getText());
            updateSpecialFields = true;
        }
        if (!nonWrappableFields.getText().trim().equals(_prefs.get("nonWrappableFields"))) {
            _prefs.put("nonWrappableFields", nonWrappableFields.getText());
            updateSpecialFields = true;
        }
        // If either of the two last entries were changed, run the update for special field handling:
        if (updateSpecialFields)
                _prefs.updateSpecialFieldHandling();

        // We want to know if the following setting has been modified:
        boolean oldShowSource = _prefs.getBoolean("showSource");
        _prefs.putBoolean("showSource", showSource.isSelected());
        if (oldShowSource != showSource.isSelected()) {
            // The source panel has been enabled or disabled, so we need to
            // remove all entry editors from cache:
            for (int j=0; j<_frame.getTabbedPane().getTabCount(); j++) {
	            BasePanel bp = (BasePanel)_frame.getTabbedPane().getComponentAt(j);
	            bp.entryEditors.clear();
            }
        }


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
            SimpleDateFormat sdf = new SimpleDateFormat(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("The chosen date format for new entries is not valid"),
                            Globals.lang("Invalid date format"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

}
