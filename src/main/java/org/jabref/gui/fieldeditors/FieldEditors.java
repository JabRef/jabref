package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FieldEditors {

    private static final Log LOGGER = LogFactory.getLog(FieldEditors.class);

    public static FieldEditorFX getForField(String fieldName, TaskExecutor taskExecutor, DialogService dialogService, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, JabRefPreferences preferences, BibDatabaseContext databaseContext, String entryType, SuggestionProviders suggestionProviders) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        AutoCompleteSuggestionProvider<?> suggestionProvider = getSuggestionProvider(fieldName, suggestionProviders, databaseContext.getMetaData());

        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName) || fieldExtras.contains(FieldProperty.DATE)) {
            if (fieldExtras.contains(FieldProperty.ISO_DATE)) {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), suggestionProvider);
            } else {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT)), suggestionProvider);
            }
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(fieldName, dialogService, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(fieldName, journalAbbreviationLoader, journalAbbreviationPreferences, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.DOI) || fieldExtras.contains(FieldProperty.EPRINT) || fieldExtras.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(fieldName, taskExecutor, dialogService, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return new OwnerEditor(fieldName, preferences, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(fieldName, dialogService, databaseContext, taskExecutor, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(fieldName, new YesNoEditorViewModel(fieldName, suggestionProvider));
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(fieldName, new MonthEditorViewModel(fieldName, suggestionProvider, databaseContext.getMode()));
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(fieldName, new GenderEditorViewModel(fieldName, suggestionProvider));
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(fieldName, new EditorTypeEditorViewModel(fieldName, suggestionProvider));
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(fieldName, new PaginationEditorViewModel(fieldName, suggestionProvider));
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            if ("patent".equalsIgnoreCase(entryType)) {
                return new OptionEditor<>(fieldName, new PatentTypeEditorViewModel(fieldName, suggestionProvider));
            } else {
                return new OptionEditor<>(fieldName, new TypeEditorViewModel(fieldName, suggestionProvider));
            }
        } else if (fieldExtras.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldExtras.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(fieldName, databaseContext, suggestionProvider);
        } else if (fieldExtras.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonsEditor(fieldName, suggestionProvider, preferences.getAutoCompletePreferences());
        }

        // default
        return new SimpleEditor(fieldName, suggestionProvider);
    }

    @SuppressWarnings("unchecked")
    private static AutoCompleteSuggestionProvider<?> getSuggestionProvider(String fieldName, SuggestionProviders suggestionProviders, MetaData metaData) {
        AutoCompleteSuggestionProvider<?> suggestionProvider = suggestionProviders.getForField(fieldName);

        List<String> contentSelectorValues = metaData.getContentSelectorValuesForField(fieldName);
        if (!contentSelectorValues.isEmpty()) {
            // Enrich auto completion by content selector values
            try {
                return new ContentSelectorSuggestionProvider((AutoCompleteSuggestionProvider<String>) suggestionProvider, contentSelectorValues);
            } catch (ClassCastException exception) {
                LOGGER.error("Content selectors are only supported for normal fields with string-based auto completion.");
                return suggestionProvider;
            }
        } else {
            return suggestionProvider;
        }
    }
}
