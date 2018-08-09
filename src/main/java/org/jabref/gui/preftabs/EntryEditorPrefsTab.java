package org.jabref.gui.preftabs;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.keyboard.EmacsKeyBindings;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;


import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_ABBREVIATED;
import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_FULL;

class EntryEditorPrefsTab extends JPanel implements PrefsTab {

    private final CheckBox autoOpenForm;
    private final CheckBox defSource;
    private final CheckBox emacsMode;
    private final CheckBox emacsRebindCtrlA;
    private final CheckBox emacsRebindCtrlF;
    private final CheckBox autoComplete;
    private final CheckBox recommendations;
    private final CheckBox validation;
    private final RadioButton autoCompBoth;
    private final RadioButton autoCompFF;
    private final RadioButton autoCompLF;
    private final RadioButton firstNameModeFull;
    private final RadioButton firstNameModeAbbr;
    private final RadioButton firstNameModeBoth;

    private final TextField autoCompFields;
    private final JabRefPreferences prefs;
    private final AutoCompletePreferences autoCompletePreferences;


    public EntryEditorPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        autoCompletePreferences = prefs.getAutoCompletePreferences();
        setLayout(new BorderLayout());

        autoOpenForm = new CheckBox(Localization.lang("Open editor when a new entry is created"));
        defSource = new CheckBox(Localization.lang("Show BibTeX source by default"));
        emacsMode = new CheckBox(Localization.lang("Use Emacs key bindings"));
        emacsRebindCtrlA = new CheckBox(Localization.lang("Rebind C-a, too"));
        emacsRebindCtrlF = new CheckBox(Localization.lang("Rebind C-f, too"));
        autoComplete = new CheckBox(Localization.lang("Enable word/name autocompletion"));
        recommendations = new CheckBox(Localization.lang("Show 'Related Articles' tab"));
        validation = new CheckBox(Localization.lang("Show validation messages"));

        // allowed name formats
        autoCompFF = new RadioButton(Localization.lang("Autocomplete names in 'Firstname Lastname' format only"));
        autoCompLF = new RadioButton(Localization.lang("Autocomplete names in 'Lastname, Firstname' format only"));
        autoCompBoth = new RadioButton(Localization.lang("Autocomplete names in both formats"));


        // treatment of first name
        firstNameModeFull = new RadioButton(Localization.lang("Use full firstname whenever possible"));
        firstNameModeAbbr = new RadioButton(Localization.lang("Use abbreviated firstname whenever possible"));
        firstNameModeBoth = new RadioButton(Localization.lang("Use abbreviated and full firstname"));

        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.setOnAction(event -> emacsRebindCtrlA.setDisable(!emacsMode.isSelected()));

        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.setOnAction(event -> emacsRebindCtrlF.setDisable(!emacsMode.isSelected()));


        autoCompFields = new TextField();
        // We need a listener on autoComplete to enable and disable the
        // autoCompFields text field:
        autoComplete.setOnAction(event -> setAutoCompleteElementsEnabled(autoComplete.isSelected()));

        GridPane builder = new GridPane();

        builder.add(new Label(Localization.lang("Editor options")),1,1);
        builder.add(new Separator(),2,1);
        builder.add(autoOpenForm, 2,2);
        builder.add(defSource, 2,3);
        builder.add(emacsMode,2,4);
        builder.add(emacsRebindCtrlA,2,5);
        builder.add(emacsRebindCtrlF, 2,6);
        builder.add(recommendations, 2,7);
        builder.add(validation, 2,8);

        builder.add(new Label(Localization.lang("Autocompletion options")),1,9);
        builder.add(autoComplete, 2,10);

        Label label = new Label(Localization.lang("Use autocompletion for the following fields") + ":");

        builder.add(label,2,11);
        builder.add(autoCompFields,3,11);

        builder.add(new Label(Localization.lang("Name format used for autocompletion")),1,14);
        builder.add(autoCompFF,2,15);
        builder.add(autoCompLF, 2,16);
        builder.add(autoCompBoth, 2,17);

        builder.add(new Label(Localization.lang("Treatment of first names")),1,18);
        builder.add(firstNameModeAbbr, 2,19);
        builder.add(firstNameModeFull,2,30);
        builder.add(firstNameModeBoth, 2,31);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void setAutoCompleteElementsEnabled(boolean enabled) {
        autoCompFields.setDisable(!enabled);
        autoCompLF.setDisable(!enabled);
        autoCompFF.setDisable(!enabled);
        autoCompBoth.setDisable(!enabled);
        firstNameModeAbbr.setDisable(!enabled);
        firstNameModeFull.setDisable(!enabled);
        firstNameModeBoth.setDisable(!enabled);
    }

    @Override
    public void setValues() {
        autoOpenForm.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM));
        defSource.setSelected(prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE));
        emacsMode.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS));
        emacsRebindCtrlA.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA));
        emacsRebindCtrlF.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF));
        recommendations.setSelected(prefs.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS));
        autoComplete.setSelected(autoCompletePreferences.shouldAutoComplete());
        autoCompFields.setText(autoCompletePreferences.getCompleteNamesAsString());

        if (autoCompletePreferences.getOnlyCompleteFirstLast()) {
            autoCompFF.setSelected(true);
        } else if (autoCompletePreferences.getOnlyCompleteLastFirst()) {
            autoCompLF.setSelected(true);
        } else {
            autoCompBoth.setSelected(true);
        }

        switch (autoCompletePreferences.getFirstNameMode()) {
        case ONLY_ABBREVIATED:
            firstNameModeAbbr.setSelected(true);
            break;
        case ONLY_FULL:
            firstNameModeFull.setSelected(true);
            break;
        default:
            firstNameModeBoth.setSelected(true);
            break;
        }

        // similar for emacs CTRL-a and emacs mode
        emacsRebindCtrlA.setDisable(!emacsMode.isSelected());
        // Autocomplete fields is only enabled when autocompletion is selected
        setAutoCompleteElementsEnabled(autoComplete.isSelected());

        validation.setSelected(prefs.getBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR));
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.AUTO_OPEN_FORM, autoOpenForm.isSelected());
        prefs.putBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE, defSource.isSelected());
        prefs.putBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS, recommendations.isSelected());
        prefs.putBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR, validation.isSelected());
        boolean emacsModeChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS) != emacsMode.isSelected();
        boolean emacsRebindCtrlAChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA) != emacsRebindCtrlA.isSelected();
        boolean emacsRebindCtrlFChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF) != emacsRebindCtrlF.isSelected();
        if (emacsModeChanged || emacsRebindCtrlAChanged || emacsRebindCtrlFChanged) {
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS, emacsMode.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA, emacsRebindCtrlA.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF, emacsRebindCtrlF.isSelected());
            // immediately apply the change
            if (emacsModeChanged) {
                if (emacsMode.isSelected()) {
                    EmacsKeyBindings.load();
                } else {
                    EmacsKeyBindings.unload();
                }
            } else {
                // only rebinding of CTRL+a or CTRL+f changed
                assert emacsMode.isSelected();
                // we simply reload the emacs mode to activate the CTRL+a/CTRL+f change
                EmacsKeyBindings.unload();
                EmacsKeyBindings.load();
            }
        }
        autoCompletePreferences.setShouldAutoComplete(autoComplete.isSelected());
        autoCompletePreferences.setCompleteNames(autoCompFields.getText());
        if (autoCompBoth.isSelected()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else if (autoCompFF.isSelected()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(true);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(true);
        }
        if (firstNameModeAbbr.isSelected()) {
            autoCompletePreferences.setFirstNameMode(ONLY_ABBREVIATED);
        } else if (firstNameModeFull.isSelected()) {
            autoCompletePreferences.setFirstNameMode(ONLY_FULL);
        } else {
            autoCompletePreferences.setFirstNameMode(AutoCompleteFirstNameMode.BOTH);
        }
        prefs.storeAutoCompletePreferences(autoCompletePreferences);
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }
}
