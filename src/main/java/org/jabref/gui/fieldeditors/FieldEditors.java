package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class FieldEditors {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldEditors.class);

    public static FieldEditorFX getForField(final Field field,
                                            final TaskExecutor taskExecutor,
                                            final DialogService dialogService,
                                            final JournalAbbreviationRepository journalAbbreviationRepository,
                                            final PreferencesService preferences,
                                            final BibDatabaseContext databaseContext,
                                            final EntryType entryType,
                                            final SuggestionProviders suggestionProviders,
                                            final UndoManager undoManager) {
        final Set<FieldProperty> fieldProperties = field.getProperties();

        final SuggestionProvider<?> suggestionProvider = getSuggestionProvider(field, suggestionProviders, databaseContext.getMetaData());

        final FieldCheckers fieldCheckers = new FieldCheckers(
                databaseContext,
                preferences.getFilePreferences(),
                journalAbbreviationRepository,
                preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex());

        boolean isMultiLine = FieldFactory.isMultiLineField(field, preferences.getFieldContentParserPreferences().getNonWrappableFields());

        if (preferences.getTimestampPreferences().getTimestampField().equals(field)) {
            return new DateEditor(field, DateTimeFormatter.ofPattern(preferences.getTimestampPreferences().getTimestampFormat()), suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.DATE)) {
            return new DateEditor(field, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(field, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldProperties.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(field, journalAbbreviationRepository, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.DOI) || fieldProperties.contains(FieldProperty.EPRINT) || fieldProperties.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(field, taskExecutor, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (field == StandardField.OWNER) {
            return new OwnerEditor(field, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(field, dialogService, databaseContext, taskExecutor, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldProperties.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(new YesNoEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldProperties.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(new MonthEditorViewModel(field, suggestionProvider, databaseContext.getMode(), fieldCheckers));
        } else if (fieldProperties.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(new GenderEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldProperties.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(new EditorTypeEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldProperties.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(new PaginationEditorViewModel(field, suggestionProvider, fieldCheckers));
        } else if (fieldProperties.contains(FieldProperty.TYPE)) {
            if (entryType.equals(IEEETranEntryType.Patent)) {
                return new OptionEditor<>(new PatentTypeEditorViewModel(field, suggestionProvider, fieldCheckers));
            } else {
                return new OptionEditor<>(new TypeEditorViewModel(field, suggestionProvider, fieldCheckers));
            }
        } else if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(field, databaseContext, (SuggestionProvider<BibEntry>) suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(field, databaseContext, (SuggestionProvider<BibEntry>) suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonsEditor(field, suggestionProvider, preferences, fieldCheckers, isMultiLine);
        } else if (StandardField.KEYWORDS.equals(field)) {
            return new KeywordsEditor(field, suggestionProvider, fieldCheckers, preferences);
        } else if (field == InternalField.KEY_FIELD) {
            return new CitationKeyEditor(field, preferences, suggestionProvider, fieldCheckers, databaseContext, undoManager, dialogService);
        } else {
            // default
            return new SimpleEditor(field, suggestionProvider, fieldCheckers, preferences, isMultiLine);
        }
    }

    private static SuggestionProvider<?> getSuggestionProvider(Field field, SuggestionProviders suggestionProviders, MetaData metaData) {
        SuggestionProvider<?> suggestionProvider = suggestionProviders.getForField(field);

        List<String> contentSelectorValues = metaData.getContentSelectorValuesForField(field);
        if (!contentSelectorValues.isEmpty()) {
            // Enrich auto completion by content selector values
            try {
                return new ContentSelectorSuggestionProvider((SuggestionProvider<String>) suggestionProvider, contentSelectorValues);
            } catch (ClassCastException exception) {
                LOGGER.error("Content selectors are only supported for normal fields with string-based auto completion.");
                return suggestionProvider;
            }
        } else {
            return suggestionProvider;
        }
    }
}
