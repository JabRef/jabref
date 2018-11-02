package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.FileDragDropPreferenceType;
import org.jabref.gui.keyboard.EmacsKeyBindings;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_ABBREVIATED;
import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_FULL;

class EntryEditorPrefsTab extends Pane implements PrefsTab {

    private final CheckBox autoOpenForm;
    private final CheckBox defSource;
    private final CheckBox emacsMode;
    private final CheckBox emacsRebindCtrlA;
    private final CheckBox emacsRebindCtrlF;
    private final CheckBox autoComplete;
    private final CheckBox recommendations;
    private final CheckBox acceptRecommendations;
    private final CheckBox validation;
    private final RadioButton autoCompBoth;
    private final RadioButton autoCompFF;
    private final RadioButton autoCompLF;
    private final RadioButton firstNameModeFull;
    private final RadioButton firstNameModeAbbr;
    private final RadioButton firstNameModeBoth;
    private final GridPane builder = new GridPane();

    private final TextField autoCompFields;
    private final JabRefPreferences prefs;
    private final AutoCompletePreferences autoCompletePreferences;

    private final RadioButton copyFile;
    private final RadioButton linkFile;
    private final RadioButton renameCopyFile;

    public EntryEditorPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        autoCompletePreferences = prefs.getAutoCompletePreferences();

        autoOpenForm = new CheckBox(Localization.lang("Open editor when a new entry is created"));
        defSource = new CheckBox(Localization.lang("Show BibTeX source by default"));
        emacsMode = new CheckBox(Localization.lang("Use Emacs key bindings"));
        emacsRebindCtrlA = new CheckBox(Localization.lang("Rebind C-a, too"));
        emacsRebindCtrlF = new CheckBox(Localization.lang("Rebind C-f, too"));
        autoComplete = new CheckBox(Localization.lang("Enable word/name autocompletion"));
        recommendations = new CheckBox(Localization.lang("Show 'Related Articles' tab"));
        acceptRecommendations = new CheckBox(Localization.lang("Accept recommendations from Mr. DLib"));
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

        Label editorOptions = new Label(Localization.lang("Editor options"));
        editorOptions.getStyleClass().add("sectionHeader");
        builder.add(editorOptions, 1, 1);
        builder.add(new Separator(), 2, 1);
        builder.add(autoOpenForm,  1, 2);
        builder.add(defSource,  1, 3);
        builder.add(emacsMode, 1, 4);
        builder.add(emacsRebindCtrlA, 1, 5);
        builder.add(emacsRebindCtrlF, 1, 6);
        builder.add(recommendations, 1, 7);
        builder.add(acceptRecommendations, 1, 8);
        builder.add(validation, 1, 9);
        builder.add(new Label(""), 1, 10);

        Label autocompletionOptions = new Label(Localization.lang("Autocompletion options"));
        autocompletionOptions.getStyleClass().add("sectionHeader");
        builder.add(autocompletionOptions, 1, 10);
        builder.add(autoComplete,   1, 11);

        Label useFields = new Label("       " + Localization.lang("Use autocompletion for the following fields") + ":");
        builder.add(useFields, 1, 12);
        builder.add(autoCompFields, 2, 12);
        builder.add(new Label(""), 1, 13);

        Label nameFormat = new Label(Localization.lang("Name format used for autocompletion"));
        nameFormat.getStyleClass().add("sectionHeader");
        final ToggleGroup autocompletionToggleGroup = new ToggleGroup();
        builder.add(nameFormat, 1, 14);
        builder.add(autoCompFF, 1, 15);
        builder.add(autoCompLF,  1, 16);
        builder.add(autoCompBoth,  1, 17);
        autoCompFF.setToggleGroup(autocompletionToggleGroup);
        autoCompLF.setToggleGroup(autocompletionToggleGroup);
        autoCompBoth.setToggleGroup(autocompletionToggleGroup);
        builder.add(new Label(""), 1, 18);

        Label treatment = new Label(Localization.lang("Treatment of first names"));
        treatment.getStyleClass().add("sectionHeader");
        final ToggleGroup treatmentOfFirstNamesToggleGroup = new ToggleGroup();
        builder.add(treatment, 1, 19);
        builder.add(firstNameModeAbbr,  1, 20);
        builder.add(firstNameModeFull, 1, 21);
        builder.add(firstNameModeBoth,  1, 22);
        firstNameModeAbbr.setToggleGroup(treatmentOfFirstNamesToggleGroup);
        firstNameModeFull.setToggleGroup(treatmentOfFirstNamesToggleGroup);
        firstNameModeBoth.setToggleGroup(treatmentOfFirstNamesToggleGroup);

        final ToggleGroup group = new ToggleGroup();
        Label linkFileOptions = new Label(Localization.lang("Default drag & drop action"));
        linkFileOptions.getStyleClass().add("sectionHeader");
        copyFile = new RadioButton(Localization.lang("Copy file to default file folder"));
        linkFile = new RadioButton(Localization.lang("Link file (without copying)"));
        renameCopyFile = new RadioButton(Localization.lang("Copy, rename and link file"));
        builder.add(linkFileOptions, 1, 23);
        builder.add(copyFile, 1, 24);
        builder.add(linkFile, 1, 25);
        builder.add(renameCopyFile, 1, 26);
        copyFile.setToggleGroup(group);
        linkFile.setToggleGroup(group);
        renameCopyFile.setToggleGroup(group);
    }

    @Override
    public Node getBuilder() {
        return builder;
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
        acceptRecommendations.setSelected(prefs.getBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS));
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

        FileDragDropPreferenceType dragDropPreferenceType = prefs.getEntryEditorFileLinkPreference();
        if (dragDropPreferenceType == FileDragDropPreferenceType.COPY) {
            copyFile.setSelected(true);
        } else if (dragDropPreferenceType == FileDragDropPreferenceType.LINK) {
            linkFile.setSelected(true);
        } else {
            renameCopyFile.setSelected(true);
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
        prefs.putBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS, acceptRecommendations.isSelected());
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

        if (copyFile.isSelected()) {
            prefs.storeEntryEditorFileLinkPreference(FileDragDropPreferenceType.COPY);
        } else if (linkFile.isSelected()) {
            prefs.storeEntryEditorFileLinkPreference(FileDragDropPreferenceType.LINK);
        } else {
            prefs.storeEntryEditorFileLinkPreference(FileDragDropPreferenceType.MOVE);
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
