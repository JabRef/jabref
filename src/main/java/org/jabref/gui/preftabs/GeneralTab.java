package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import static org.jabref.logic.l10n.Languages.LANGUAGES;

class GeneralTab extends JPanel implements PrefsTab {

    private final JCheckBox useOwner;
    private final JCheckBox overwriteOwner;
    private final JCheckBox enforceLegalKeys;
    private final JCheckBox shouldCollectTelemetry;
    private final JCheckBox confirmDelete;
    private final JCheckBox memoryStick;
    private final JCheckBox inspectionWarnDupli;
    private final JCheckBox useTimeStamp;
    private final JCheckBox updateTimeStamp;
    private final JCheckBox overwriteTimeStamp;
    private final JCheckBox markImportedEntries;
    private final JCheckBox unmarkAllEntriesBeforeImporting;
    private final JTextField defOwnerField;

    private final JTextField timeStampFormat;
    private final JTextField timeStampField;
    private final JabRefPreferences prefs;
    private final JComboBox<String> language = new JComboBox<>(LANGUAGES.keySet().toArray(new String[LANGUAGES.keySet().size()]));
    private final JComboBox<Charset> encodings;
    private final JComboBox<BibDatabaseMode> biblatexMode;

    public class DefaultBibModeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(((BibDatabaseMode) value).getFormattedName());
            return this;
        }
    }


    public GeneralTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        setLayout(new BorderLayout());

        biblatexMode = new JComboBox<>(BibDatabaseMode.values());
        biblatexMode.setRenderer(new DefaultBibModeRenderer());

        memoryStick = new JCheckBox(Localization.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        useOwner = new JCheckBox(Localization.lang("Mark new entries with owner name") + ':');
        updateTimeStamp = new JCheckBox(Localization.lang("Update timestamp on modification"));
        useTimeStamp = new JCheckBox(Localization.lang("Mark new entries with addition date") + ". "
                + Localization.lang("Date format") + ':');
        useTimeStamp.addChangeListener(e -> updateTimeStamp.setEnabled(useTimeStamp.isSelected()));
        overwriteOwner = new JCheckBox(Localization.lang("Overwrite"));
        overwriteTimeStamp = new JCheckBox(Localization.lang("Overwrite"));
        overwriteOwner.setToolTipText(Localization.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        overwriteTimeStamp.setToolTipText(Localization.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        enforceLegalKeys = new JCheckBox(Localization.lang("Enforce legal characters in BibTeX keys"));
        confirmDelete = new JCheckBox(Localization.lang("Show confirmation dialog when deleting entries"));

        markImportedEntries = new JCheckBox(Localization.lang("Mark entries imported into an existing library"));
        unmarkAllEntriesBeforeImporting = new JCheckBox(Localization.lang("Unmark all entries before importing new entries into an existing library"));
        defOwnerField = new JTextField();
        timeStampFormat = new JTextField();
        timeStampField = new JTextField();
        inspectionWarnDupli = new JCheckBox(Localization.lang("Warn about unresolved duplicates when closing inspection window"));

        shouldCollectTelemetry = new JCheckBox(Localization.lang("Collect and share telemetry data to help improve JabRef."));

        encodings = new JComboBox<>();
        encodings.setModel(new DefaultComboBoxModel<>(Encodings.ENCODINGS));

        FormLayout layout = new FormLayout(
                "8dlu, 1dlu, left:pref:grow, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, left:pref, 1dlu, left:pref, 4dlu, left:pref",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("General"));
        builder.nextLine();
        builder.append(inspectionWarnDupli, 13);
        builder.nextLine();
        builder.append(confirmDelete, 13);
        builder.nextLine();
        builder.append(enforceLegalKeys, 13);
        builder.nextLine();
        builder.append(memoryStick, 13);

        // Create a new panel with its own FormLayout for the last items:
        builder.append(useOwner, 3);
        builder.append(defOwnerField);
        builder.append(overwriteOwner);
        builder.append(new JPanel(), 3);

        JButton help = new HelpAction(HelpFile.OWNER).getHelpButton();
        builder.append(help);
        builder.nextLine();

        builder.append(useTimeStamp, 3);
        builder.append(timeStampFormat);
        builder.append(overwriteTimeStamp);
        builder.append(Localization.lang("Field name") + ':');
        builder.append(timeStampField);

        help = new HelpAction(HelpFile.TIMESTAMP).getHelpButton();
        builder.append(help);
        builder.nextLine();

        builder.append(new JPanel());
        builder.append(updateTimeStamp, 11);
        builder.nextLine();

        builder.append(markImportedEntries, 13);
        builder.nextLine();
        builder.append(unmarkAllEntriesBeforeImporting, 13);
        builder.nextLine();
        builder.append(shouldCollectTelemetry, 13);
        builder.nextLine();
        JLabel lab;
        lab = new JLabel(Localization.lang("Language") + ':');
        builder.append(lab, 3);
        builder.append(language);
        builder.nextLine();
        lab = new JLabel(Localization.lang("Default encoding") + ':');
        builder.append(lab, 3);
        builder.append(encodings);

        builder.nextLine();
        builder.appendSeparator(Localization.lang("Default bibliography mode"));
        builder.append(new JPanel());
        builder.append(biblatexMode);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {
        useOwner.setSelected(prefs.getBoolean(JabRefPreferences.USE_OWNER));
        overwriteOwner.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER));
        useTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        overwriteTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        updateTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
        updateTimeStamp.setEnabled(useTimeStamp.isSelected());
        enforceLegalKeys.setSelected(prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        shouldCollectTelemetry.setSelected(prefs.shouldCollectTelemetry());
        memoryStick.setSelected(prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        confirmDelete.setSelected(prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        defOwnerField.setText(prefs.get(JabRefPreferences.DEFAULT_OWNER));
        timeStampFormat.setText(prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        timeStampField.setText(prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        inspectionWarnDupli.setSelected(prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        markImportedEntries.setSelected(prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));
        unmarkAllEntriesBeforeImporting.setSelected(prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING));
        if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            biblatexMode.setSelectedItem(BibDatabaseMode.BIBLATEX);
        } else {
            biblatexMode.setSelectedItem(BibDatabaseMode.BIBTEX);
        }

        Charset enc = Globals.prefs.getDefaultEncoding();
        encodings.setSelectedItem(enc);

        String oldLan = prefs.get(JabRefPreferences.LANGUAGE);

        // Language choice
        int ilk = 0;
        for (String lan : LANGUAGES.values()) {
            if (lan.equals(oldLan)) {
                language.setSelectedIndex(ilk);
            }
            ilk++;
        }

    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.USE_OWNER, useOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_OWNER, overwriteOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.USE_TIME_STAMP, useTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP, overwriteTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.UPDATE_TIMESTAMP, updateTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY, enforceLegalKeys.isSelected());
        prefs.setShouldCollectTelemetry(shouldCollectTelemetry.isSelected());
        if (prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE) && !memoryStick.isSelected()) {
            JOptionPane.showMessageDialog(null, Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."),
                    Localization.lang("Memory stick mode"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        prefs.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStick.isSelected());
        prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDelete.isSelected());
        prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarnDupli.isSelected());
        String owner = defOwnerField.getText().trim();
        prefs.put(JabRefPreferences.DEFAULT_OWNER, owner);
        prefs.put(JabRefPreferences.TIME_STAMP_FORMAT, timeStampFormat.getText().trim());
        prefs.put(JabRefPreferences.TIME_STAMP_FIELD, timeStampField.getText().trim());
        // Update name of the time stamp field based on preferences
        InternalBibtexFields.updateTimeStampField(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        prefs.setDefaultEncoding((Charset) encodings.getSelectedItem());
        prefs.putBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES, markImportedEntries.isSelected());
        prefs.putBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING, unmarkAllEntriesBeforeImporting.isSelected());
        prefs.putBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE, biblatexMode.getSelectedItem() == BibDatabaseMode.BIBLATEX);

        if (!LANGUAGES.get(language.getSelectedItem()).equals(prefs.get(JabRefPreferences.LANGUAGE))) {
            prefs.put(JabRefPreferences.LANGUAGE, LANGUAGES.get(language.getSelectedItem()));
            Localization.setLanguage(LANGUAGES.get(language.getSelectedItem()));
            // Update any defaults that might be language dependent:
            Globals.prefs.setLanguageDependentDefaultValues();
            // Warn about restart needed:
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You have changed the language setting.")
                            .concat(" ")
                            .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                    Localization.lang("Changed language settings"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public boolean validateSettings() {
        try {
            // Test if date format is legal:
            DateTimeFormatter.ofPattern(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            JOptionPane.showMessageDialog
                    (null, Localization.lang("The chosen date format for new entries is not valid"),
                            Localization.lang("Invalid date format"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }
}
