package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

public class FieldEditors {

    public static FieldEditorFX getForField(String fieldName, TaskExecutor taskExecutor, DialogService dialogService, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, JabRefPreferences preferences, BibDatabaseContext databaseContext, String entryType, SuggestionProviders suggestionProviders) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        AutoCompleteSuggestionProvider<?> suggestionProvider = suggestionProviders.getForField(fieldName);

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
            return new PersonsEditor(fieldName, suggestionProvider, new AutoCompletePreferences(preferences));
        }

        // default
        return new SimpleEditor(fieldName, suggestionProvider);
    }
}
