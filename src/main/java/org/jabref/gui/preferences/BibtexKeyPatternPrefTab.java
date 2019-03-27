package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.preferences.JabRefPreferences;

/**
 * The Preferences panel for key generation.
 */
class BibtexKeyPatternPrefTab extends BibtexKeyPatternPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private final GridPane builder = new GridPane();
    private final CheckBox dontOverwrite = new CheckBox(Localization.lang("Do not overwrite existing keys"));
    private final CheckBox warnBeforeOverwriting = new CheckBox(Localization.lang("Warn before overwriting existing keys"));
    private final CheckBox generateOnSave = new CheckBox(Localization.lang("Generate keys before saving (for entries without a key)"));

    private final RadioButton letterStartA = new RadioButton(Localization.lang("Ensure unique keys using letters (a, b, ...)"));
    private final RadioButton letterStartB = new RadioButton(Localization.lang("Ensure unique keys using letters (b, c, ...)"));
    private final RadioButton alwaysAddLetter = new RadioButton(Localization.lang("Always add letter (a, b, ...) to generated keys"));

    private final TextField keyPatternRegex = new TextField();
    private final TextField keyPatternReplacement = new TextField();


    public BibtexKeyPatternPrefTab(JabRefPreferences prefs, BasePanel panel) {
        super(panel);
        builder.add(super.getPanel(), 1, 1);
        builder.add(new Label(""), 1, 2);
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
        // Build a panel for checkbox settings:
        Label keyGeneratorSettings = new Label(Localization.lang("Key generator settings"));
        keyGeneratorSettings.getStyleClass().add("sectionHeader");
        builder.add(keyGeneratorSettings, 1, 10);
        builder.add(letterStartA, 2, 11);
        builder.add(warnBeforeOverwriting, 1, 12);
        builder.add(letterStartB, 2, 12);
        builder.add(dontOverwrite, 1, 13);
        builder.add(alwaysAddLetter, 2, 13);
        builder.add(generateOnSave, 1, 14);

        builder.add((new Label(Localization.lang("Replace (regular expression)") + ':')), 1, 15);
        builder.add(new Label(Localization.lang("by") + ':'), 2, 15);

        builder.add(keyPatternRegex, 1, 16);
        builder.add(keyPatternReplacement, 2, 16);

        dontOverwrite.setOnAction(e ->
        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setDisable(dontOverwrite.isSelected()));
    }

    @Override
    public Node getBuilder() {
        return builder;
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
        warnBeforeOverwriting.setDisable(dontOverwrite.isSelected());

        keyPatternRegex.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REGEX));
        keyPatternReplacement.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT));

    }

    @Override
    public String getTabName() {
        return Localization.lang("BibTeX key generator");
    }
}
