package org.jabref.gui.preferences.entryeditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class EntryEditorTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openOnNewEntryProperty = new SimpleBooleanProperty();
    private final BooleanProperty defaultSourceProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableRelatedArticlesTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableAiSummaryTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableAiChatTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty acceptRecommendationsProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableLatexCitationsTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty smartFileAnnotationsTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableValidationProperty = new SimpleBooleanProperty();
    private final BooleanProperty allowIntegerEditionProperty = new SimpleBooleanProperty();
    private final BooleanProperty journalPopupProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoLinkEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableSciteTabProperty = new SimpleBooleanProperty();

    private final BooleanProperty showUserCommentsProperty = new SimpleBooleanProperty();

    private final StringProperty fieldsProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final MrDlibPreferences mrDlibPreferences;

    public EntryEditorTabViewModel(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
        this.mrDlibPreferences = preferences.getMrDlibPreferences();
    }

    @Override
    public void setValues() {
        // ToDo: Include CustomizeGeneralFieldsDialog in PreferencesDialog
        //       Therefore yet unused: entryEditorPreferences.getEntryEditorTabList();

        openOnNewEntryProperty.setValue(entryEditorPreferences.shouldOpenOnNewEntry());
        defaultSourceProperty.setValue(entryEditorPreferences.showSourceTabByDefault());
        enableRelatedArticlesTabProperty.setValue(entryEditorPreferences.shouldShowRecommendationsTab());
        enableAiSummaryTabProperty.setValue(entryEditorPreferences.shouldShowAiSummaryTab());
        enableAiChatTabProperty.setValue(entryEditorPreferences.shouldShowAiChatTab());
        acceptRecommendationsProperty.setValue(mrDlibPreferences.shouldAcceptRecommendations());
        enableLatexCitationsTabProperty.setValue(entryEditorPreferences.shouldShowLatexCitationsTab());
        smartFileAnnotationsTabProperty.setValue(entryEditorPreferences.shouldShowFileAnnotationsTab());
        enableValidationProperty.setValue(entryEditorPreferences.shouldEnableValidation());
        allowIntegerEditionProperty.setValue(entryEditorPreferences.shouldAllowIntegerEditionBibtex());
        journalPopupProperty.setValue(entryEditorPreferences.shouldEnableJournalPopup() == EntryEditorPreferences.JournalPopupEnabled.ENABLED);
        autoLinkEnabledProperty.setValue(entryEditorPreferences.autoLinkFilesEnabled());
        enableSciteTabProperty.setValue(entryEditorPreferences.shouldShowSciteTab());
        showUserCommentsProperty.setValue(entryEditorPreferences.shouldShowUserCommentsFields());

        setFields(entryEditorPreferences.getEntryEditorTabs());
    }

    public void resetToDefaults() {
        setFields(preferences.getEntryEditorPreferences().getDefaultEntryEditorTabs());
    }

    private void setFields(Map<String, Set<Field>> tabNamesAndFields) {
        StringBuilder sb = new StringBuilder();

        // Fill with customized vars
        for (Map.Entry<String, Set<Field>> tab : tabNamesAndFields.entrySet()) {
            sb.append(tab.getKey());
            sb.append(':');
            sb.append(FieldFactory.serializeFieldsList(tab.getValue()));
            sb.append('\n');
        }
        fieldsProperty.set(sb.toString());
    }

    @Override
    public void storeSettings() {
        // entryEditorPreferences.setEntryEditorTabList();
        entryEditorPreferences.setShouldOpenOnNewEntry(openOnNewEntryProperty.getValue());
        entryEditorPreferences.setShouldShowRecommendationsTab(enableRelatedArticlesTabProperty.getValue());
        entryEditorPreferences.setShouldShowAiSummaryTab(enableAiSummaryTabProperty.getValue());
        entryEditorPreferences.setShouldShowAiChatTab(enableAiChatTabProperty.getValue());
        mrDlibPreferences.setAcceptRecommendations(acceptRecommendationsProperty.getValue());
        entryEditorPreferences.setShouldShowLatexCitationsTab(enableLatexCitationsTabProperty.getValue());
        entryEditorPreferences.setShouldShowFileAnnotationsTab(smartFileAnnotationsTabProperty.getValue());
        entryEditorPreferences.setShowSourceTabByDefault(defaultSourceProperty.getValue());
        entryEditorPreferences.setEnableValidation(enableValidationProperty.getValue());
        entryEditorPreferences.setAllowIntegerEditionBibtex(allowIntegerEditionProperty.getValue());
        entryEditorPreferences.setEnableJournalPopup(journalPopupProperty.getValue()
                ? EntryEditorPreferences.JournalPopupEnabled.ENABLED
                : EntryEditorPreferences.JournalPopupEnabled.DISABLED);
        // entryEditorPreferences.setDividerPosition();
        entryEditorPreferences.setAutoLinkFilesEnabled(autoLinkEnabledProperty.getValue());
        entryEditorPreferences.setShouldShowSciteTab(enableSciteTabProperty.getValue());
        entryEditorPreferences.setShowUserCommentsFields(showUserCommentsProperty.getValue());

        Map<String, Set<Field>> customTabsMap = new LinkedHashMap<>();
        String[] lines = fieldsProperty.get().split("\n");

        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length != 2) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Error"),
                        Localization.lang("Each line must be of the following form: 'tab:field1;field2;...;fieldN'."));
                return;
            }

            // Use literal string of unwanted characters specified below as opposed to exporting characters
            // from preferences because the list of allowable characters in this particular differs
            // i.e. ';' character is allowed in this window, but it's on the list of unwanted chars in preferences
            String unwantedChars = "#{}()~,^&-\"'`ʹ\\";
            String testString = CitationKeyGenerator.cleanKey(parts[1], unwantedChars);
            if (!testString.equals(parts[1])) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Error"),
                        Localization.lang("Field names are not allowed to contain white spaces or certain characters (%0).",
                                "# { } ( ) ~ , ^ & - \" ' ` ʹ \\"));
                return;
            }

            customTabsMap.put(parts[0], FieldFactory.parseFieldList(parts[1]));
        }

        entryEditorPreferences.setEntryEditorTabList(customTabsMap);
    }

    public BooleanProperty openOnNewEntryProperty() {
        return openOnNewEntryProperty;
    }

    public BooleanProperty defaultSourceProperty() {
        return defaultSourceProperty;
    }

    public BooleanProperty enableRelatedArticlesTabProperty() {
        return enableRelatedArticlesTabProperty;
    }

    public BooleanProperty enableAiSummaryTabProperty() {
        return enableAiSummaryTabProperty;
    }

    public BooleanProperty enableAiChatTabProperty() {
        return enableAiChatTabProperty;
    }

    public BooleanProperty acceptRecommendationsProperty() {
        return acceptRecommendationsProperty;
    }

    public BooleanProperty enableLatexCitationsTabProperty() {
        return enableLatexCitationsTabProperty;
    }

    public BooleanProperty smartFileAnnotationsTabProperty() {
        return smartFileAnnotationsTabProperty;
    }

    public BooleanProperty enableValidationProperty() {
        return enableValidationProperty;
    }

    public BooleanProperty allowIntegerEditionProperty() {
        return this.allowIntegerEditionProperty;
    }

    public BooleanProperty journalPopupProperty() {
        return journalPopupProperty;
    }

    public StringProperty fieldsProperty() {
        return fieldsProperty;
    }

    public BooleanProperty autoLinkFilesEnabledProperty() {
        return autoLinkEnabledProperty;
    }

    public BooleanProperty enableSciteTabProperty() {
        return enableSciteTabProperty;
    }

    public BooleanProperty showUserCommentsProperty() {
        return this.showUserCommentsProperty;
    }
}
