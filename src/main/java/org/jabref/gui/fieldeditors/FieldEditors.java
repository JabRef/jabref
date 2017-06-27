package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

public class FieldEditors {

    public static FieldEditorFX getForField(String fieldName, TaskExecutor taskExecutor, DialogService dialogService, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, JabRefPreferences preferences, BibDatabaseContext databaseContext, String entryType, ContentAutoCompleters autoCompleter) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName) || fieldExtras.contains(FieldProperty.DATE)) {
            if (fieldExtras.contains(FieldProperty.ISO_DATE)) {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), autoCompleter);
            } else {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT)), autoCompleter);
            }
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(fieldName, dialogService, autoCompleter);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(fieldName, journalAbbreviationLoader, journalAbbreviationPreferences, autoCompleter);
        } else if (fieldExtras.contains(FieldProperty.DOI) || fieldExtras.contains(FieldProperty.EPRINT) || fieldExtras.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(fieldName, taskExecutor, dialogService, autoCompleter);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return new OwnerEditor(fieldName, preferences, autoCompleter);
        } else if (fieldExtras.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(fieldName, dialogService, databaseContext, taskExecutor, autoCompleter);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(fieldName, new YesNoEditorViewModel(fieldName, autoCompleter));
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(fieldName, new MonthEditorViewModel(fieldName, autoCompleter, databaseContext.getMode()));
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(fieldName, new GenderEditorViewModel(fieldName, autoCompleter));
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(fieldName, new EditorTypeEditorViewModel(fieldName, autoCompleter));
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(fieldName, new PaginationEditorViewModel(fieldName, autoCompleter));
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            if ("patent".equalsIgnoreCase(entryType)) {
                return new OptionEditor<>(fieldName, new PatentTypeEditorViewModel(fieldName, autoCompleter));
            } else {
                return new OptionEditor<>(fieldName, new TypeEditorViewModel(fieldName, autoCompleter));
            }
        } else if (fieldExtras.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldExtras.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(fieldName, databaseContext, autoCompleter);
        }

        // default
        return new SimpleEditor(fieldName, autoCompleter);
    }
}
