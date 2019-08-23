package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

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
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_ABBREVIATED;
import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_FULL;

class EntryEditorPrefsTab extends Pane implements PreferencesTab {

    private final CheckBox autoOpenForm;
    private final CheckBox defSource;
    private final CheckBox emacsMode;
    private final CheckBox emacsRebindCtrlA;
    private final CheckBox emacsRebindCtrlF;
    private final CheckBox autoComplete;
    private final CheckBox recommendations;
    private final CheckBox acceptRecommendations;
    private final CheckBox latexCitations;
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
        builder.setVgap(7);

        autoOpenForm = new CheckBox(Localization.lang("Open editor when a new entry is created"));
        defSource = new CheckBox(Localization.lang("Show BibTeX source by default"));
        emacsMode = new CheckBox(Localization.lang("Use Emacs key bindings"));
        emacsRebindCtrlA = new CheckBox(Localization.lang("Rebind C-a, too"));
        emacsRebindCtrlF = new CheckBox(Localization.lang("Rebind C-f, too"));
        autoComplete = new CheckBox(Localization.lang("Enable word/name autocompletion"));
        recommendations = new CheckBox(Localization.lang("Show 'Related Articles' tab"));
        acceptRecommendations = new CheckBox(Localization.lang("Accept recommendations from Mr. DLib"));
        latexCitations = new CheckBox(Localization.lang("Show 'LaTeX Citations' tab"));
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

        // Editor options title
        Label editorOptions = new Label(Localization.lang("Editor options"));
        editorOptions.getStyleClass().add("sectionHeader");
        builder.add(editorOptions, 1, 1);

        // Editor options configuration
        builder.add(autoOpenForm,  1, 2);
        builder.add(defSource,  1, 3);
        builder.add(emacsMode, 1, 4);
        builder.add(emacsRebindCtrlA, 1, 5);
        builder.add(emacsRebindCtrlF, 1, 6);
        builder.add(recommendations, 1, 7);
        builder.add(acceptRecommendations, 1, 8);
        builder.add(latexCitations, 1, 9);
        builder.add(validation, 1, 10);
        builder.add(new Label(""), 1, 11);

        builder.add(new Separator(), 1, 13);

        // Autocompletion options title
        Label autocompletionOptions = new Label(Localization.lang("Autocompletion options"));
        autocompletionOptions.getStyleClass().add("sectionHeader");
        builder.add(autocompletionOptions, 1, 15);
        builder.add(autoComplete,   1, 16);

        Label useFields = new Label("       " + Localization.lang("Use autocompletion for the following fields") + ":");
        builder.add(useFields, 1, 17);
        builder.add(autoCompFields, 2, 17);
        builder.add(new Label(""), 1, 18);

        builder.add(new Separator(), 1, 21);

        // Name format title
        Label nameFormat = new Label(Localization.lang("Name format used for autocompletion"));
        nameFormat.getStyleClass().add("sectionHeader");
        builder.add(nameFormat, 1, 23);

        // Name format configuration
        final ToggleGroup autocompletionToggleGroup = new ToggleGroup();
        builder.add(autoCompFF, 1, 24);
        builder.add(autoCompLF,  1, 25);
        builder.add(autoCompBoth,  1, 26);
        autoCompFF.setToggleGroup(autocompletionToggleGroup);
        autoCompLF.setToggleGroup(autocompletionToggleGroup);
        autoCompBoth.setToggleGroup(autocompletionToggleGroup);
        builder.add(new Label(""), 1, 27);

        builder.add(new Separator(), 1, 30);

        // Treatement of first names title
        Label treatment = new Label(Localization.lang("Treatment of first names"));
        treatment.getStyleClass().add("sectionHeader");
        builder.add(treatment, 1, 32);

        // Treatment of first names configuration
        final ToggleGroup treatmentOfFirstNamesToggleGroup = new ToggleGroup();
        builder.add(firstNameModeAbbr,  1, 33);
        builder.add(firstNameModeFull, 1, 34);
        builder.add(firstNameModeBoth,  1, 35);
        firstNameModeAbbr.setToggleGroup(treatmentOfFirstNamesToggleGroup);
        firstNameModeFull.setToggleGroup(treatmentOfFirstNamesToggleGroup);
        firstNameModeBoth.setToggleGroup(treatmentOfFirstNamesToggleGroup);

        builder.add(new Separator(), 1, 38);

        // Default drag & drop title
        Label linkFileOptions = new Label(Localization.lang("Default drag & drop action"));
        linkFileOptions.getStyleClass().add("sectionHeader");
        builder.add(linkFileOptions, 1, 40);

        // Default drag & drop configuration
        final ToggleGroup group = new ToggleGroup();
        copyFile = new RadioButton(Localization.lang("Copy file to default file folder"));
        linkFile = new RadioButton(Localization.lang("Link file (without copying)"));
        renameCopyFile = new RadioButton(Localization.lang("Copy, rename and link file"));
        builder.add(copyFile, 1, 41);
        builder.add(linkFile, 1, 42);
        builder.add(renameCopyFile, 1, 43);
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
        latexCitations.setSelected(prefs.getBoolean(JabRefPreferences.SHOW_LATEX_CITATIONS));
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
        prefs.putBoolean(JabRefPreferences.SHOW_LATEX_CITATIONS, latexCitations.isSelected());
        prefs.putBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR, validation.isSelected());

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

    @Override
    public List<String> getRestartWarnings() { return new ArrayList<>(); }
}
