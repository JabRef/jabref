package org.jabref.gui.preftabs;

import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The Preferences panel for key generation.
 */
class BibtexKeyPatternPrefTab extends BibtexKeyPatternPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JCheckBox dontOverwrite = new JCheckBox(Localization.lang("Do not overwrite existing keys"));
    private final JCheckBox warnBeforeOverwriting = new JCheckBox(Localization.lang("Warn before overwriting existing keys"));
    private final JCheckBox generateOnSave = new JCheckBox(Localization.lang("Generate keys before saving (for entries without a key)"));
    private final JCheckBox autoGenerateOnImport = new JCheckBox(Localization.lang("Generate keys for imported entries"));

    private final JRadioButton letterStartA = new JRadioButton(Localization.lang("Ensure unique keys using letters (a, b, ...)"));
    private final JRadioButton letterStartB = new JRadioButton(Localization.lang("Ensure unique keys using letters (b, c, ...)"));
    private final JRadioButton alwaysAddLetter = new JRadioButton(Localization.lang("Always add letter (a, b, ...) to generated keys"));

    private final JTextField keyPatternRegex = new JTextField(20);
    private final JTextField keyPatternReplacement = new JTextField(20);


    public BibtexKeyPatternPrefTab(JabRefPreferences prefs, BasePanel panel) {
        super(panel);
        this.prefs = prefs;
        appendKeyGeneratorSettings();
    }

    /**
     * Store changes to table preferences. This method is called when the user clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        // Set the default value:
        Globals.prefs.put(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN, defaultPat.getText());

        Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, warnBeforeOverwriting.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY, dontOverwrite.isSelected());

        Globals.prefs.put(JabRefPreferences.KEY_PATTERN_REGEX, keyPatternRegex.getText());
        Globals.prefs.put(JabRefPreferences.KEY_PATTERN_REPLACEMENT, keyPatternReplacement.getText());
        Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION, autoGenerateOnImport.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING, generateOnSave.isSelected());

        if (alwaysAddLetter.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, true);
        } else if (letterStartA.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, true);
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
        }
        else {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, false);
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
        }

        // fetch entries from GUI
        GlobalBibtexKeyPattern keypatterns = getKeyPatternAsGlobalBibtexKeyPattern();
        // store new patterns globally
        prefs.putKeyPattern(keypatterns);
    }

    private void appendKeyGeneratorSettings() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(letterStartA);
        bg.add(letterStartB);
        bg.add(alwaysAddLetter);

        // Build a panel for checkbox settings:
        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 8dlu, left:pref", "");
        JPanel pan = new JPanel();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Localization.lang("Key generator settings"));

        builder.nextLine();
        builder.append(pan);
        builder.append(autoGenerateOnImport);
        builder.append(letterStartA);
        builder.nextLine();
        builder.append(pan);
        builder.append(warnBeforeOverwriting);
        builder.append(letterStartB);
        builder.nextLine();
        builder.append(pan);
        builder.append(dontOverwrite);
        builder.append(alwaysAddLetter);
        builder.nextLine();
        builder.append(pan);
        builder.append(generateOnSave);
        builder.nextLine();
        builder.append(pan);
        builder.append(Localization.lang("Replace (regular expression)") + ':');
        builder.append(Localization.lang("by") + ':');

        builder.nextLine();
        builder.append(pan);
        builder.append(keyPatternRegex);
        builder.append(keyPatternReplacement);

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        con.gridx = 1;
        con.gridy = 3;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(builder.getPanel(), con);
        add(builder.getPanel());

        dontOverwrite.addChangeListener(e ->
        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected()));
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public void setValues() {
        super.setValues(Globals.prefs.getKeyPattern());
        defaultPat.setText(Globals.prefs.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        dontOverwrite.setSelected(Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY));
        generateOnSave.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING));
        autoGenerateOnImport.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION));
        warnBeforeOverwriting.setSelected(Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY));

        boolean prefAlwaysAddLetter = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER);
        boolean firstLetterA = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A);
        if (prefAlwaysAddLetter) {
            this.alwaysAddLetter.setSelected(true);
        } else if (firstLetterA) {
            this.letterStartA.setSelected(true);
        } else {
            this.letterStartB.setSelected(true);
        }

        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected());

        keyPatternRegex.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REGEX));
        keyPatternReplacement.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT));

    }

    @Override
    public String getTabName() {
        return Localization.lang("BibTeX key generator");
    }
}
