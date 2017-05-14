package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

public class FieldEditors {

    public static FieldEditorFX getForField(String fieldName, TaskExecutor taskExecutor, DialogService dialogService, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, JabRefPreferences preferences, BibDatabaseContext databaseContext, String entryType) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName) || fieldExtras.contains(FieldProperty.DATE)) {
            if (fieldExtras.contains(FieldProperty.ISO_DATE)) {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"));
            } else {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT)));
            }
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(fieldName, dialogService);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(fieldName, journalAbbreviationLoader, journalAbbreviationPreferences);
        } else if (fieldExtras.contains(FieldProperty.DOI) || fieldExtras.contains(FieldProperty.EPRINT) || fieldExtras.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(fieldName, taskExecutor, dialogService);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return new OwnerEditor(fieldName, preferences);
        } else if (fieldExtras.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(fieldName, dialogService, databaseContext, taskExecutor);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(fieldName, new YesNoEditorViewModel());
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(fieldName, new MonthEditorViewModel(databaseContext.getMode()));
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(fieldName, new GenderEditorViewModel());
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(fieldName, new EditorTypeEditorViewModel());
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(fieldName, new PaginationEditorViewModel());
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            if ("patent".equalsIgnoreCase(entryType)) {
                return new OptionEditor<>(fieldName, new PatentTypeEditorViewModel());
            } else {
                return new OptionEditor<>(fieldName, new TypeEditorViewModel());
            }
        } else if (fieldExtras.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldExtras.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(fieldName, databaseContext);
        }

        // default
        return new SimpleEditor(fieldName);
    }
}
