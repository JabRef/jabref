package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldEditors {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldEditors.class);

    public static FieldEditorFX getForField(final Field field,
                                            final TaskExecutor taskExecutor,
                                            final DialogService dialogService,
                                            final JournalAbbreviationRepository journalAbbreviationRepository,
                                            final JabRefPreferences preferences,
                                            final BibDatabaseContext databaseContext,
                                            final String entryType,
                                            final SuggestionProviders suggestionProviders,
                                            final UndoManager undoManager) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(field);

        final AutoCompleteSuggestionProvider<?> suggestionProvider = getSuggestionProvider(field, suggestionProviders, databaseContext.getMetaData());

        final FieldCheckers fieldCheckers = new FieldCheckers(
                databaseContext,
                preferences.getFilePreferences(),
                journalAbbreviationRepository,
                preferences.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));

        final boolean isSingleLine = InternalBibtexFields.isSingleLineField(field);

        if (preferences.getTimestampPreferences().getTimestampField().equals(field) || fieldExtras.contains(FieldProperty.DATE)) {
            if (fieldExtras.contains(FieldProperty.ISO_DATE)) {
                return new DateEditor(field, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), suggestionProvider, fieldCheckers);
            } else {
                return new DateEditor(field, DateTimeFormatter.ofPattern(Globals.prefs.getTimestampPreferences().getTimestampFormat()), suggestionProvider, fieldCheckers);
            }
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(field, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(field, journalAbbreviationRepository, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.DOI) || fieldExtras.contains(FieldProperty.EPRINT) || fieldExtras.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(field, taskExecutor, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return new OwnerEditor(field, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(field, dialogService, databaseContext, taskExecutor, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(new YesNoEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(new MonthEditorViewModel(field, suggestionProvider, databaseContext.getMode(), fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(new GenderEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(new EditorTypeEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(new PaginationEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            if ("patent".equalsIgnoreCase(entryType)) {
                return new OptionEditor<>(new PatentTypeEditorViewModel(field, suggestionProvider, fieldCheckers));
            } else {
                return new OptionEditor<>(new TypeEditorViewModel(field, suggestionProvider, fieldCheckers));
            }
        } else if (fieldExtras.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldExtras.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(field, databaseContext, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonsEditor(field, suggestionProvider, preferences, fieldCheckers, isSingleLine);
        } else if (StandardField.KEYWORDS.equals(field)) {
            return new KeywordsEditor(field, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.MULTILINE_TEXT)) {
            return new MultilineEditor(field, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.KEY)) {
            return new BibtexKeyEditor(field, preferences, suggestionProvider, fieldCheckers, databaseContext, undoManager, dialogService);
        }

        // default
        return new SimpleEditor(field, suggestionProvider, fieldCheckers, preferences, isSingleLine);
    }

    @SuppressWarnings("unchecked")
    private static AutoCompleteSuggestionProvider<?> getSuggestionProvider(Field field, SuggestionProviders suggestionProviders, MetaData metaData) {
        AutoCompleteSuggestionProvider<?> suggestionProvider = suggestionProviders.getForField(field);

        List<String> contentSelectorValues = metaData.getContentSelectorValuesForField(field);
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
