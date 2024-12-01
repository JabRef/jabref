package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.identifier.IdentifierEditor;
import org.jabref.gui.fieldeditors.optioneditors.LanguageEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.MonthEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.OptionEditor;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.CustomFieldEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.EditorTypeEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.GenderEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.PaginationEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.PatentTypeEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.TypeEditorViewModel;
import org.jabref.gui.fieldeditors.optioneditors.mapbased.YesNoEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.MetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class FieldEditors {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldEditors.class);

    public static FieldEditorFX getForField(final Field field,
                                            final JournalAbbreviationRepository journalAbbreviationRepository,
                                            final GuiPreferences preferences,
                                            final BibDatabaseContext databaseContext,
                                            final EntryType entryType,
                                            final SuggestionProviders suggestionProviders,
                                            final UndoManager undoManager,
                                            final UndoAction undoAction,
                                            final RedoAction redoAction) {
        final Set<FieldProperty> fieldProperties = field.getProperties();

        final SuggestionProvider<?> suggestionProvider = getSuggestionProvider(field, suggestionProviders, databaseContext.getMetaData());

        final FieldCheckers fieldCheckers = new FieldCheckers(
                databaseContext,
                preferences.getFilePreferences(),
                journalAbbreviationRepository,
                preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex());

        boolean isMultiLine = FieldFactory.isMultiLineField(field, preferences.getFieldPreferences().getNonWrappableFields());

        if (preferences.getTimestampPreferences().getTimestampField().equals(field)) {
            return new DateEditor(field, DateTimeFormatter.ofPattern(preferences.getTimestampPreferences().getTimestampFormat()), suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (fieldProperties.contains(FieldProperty.DATE)) {
            return new DateEditor(field, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (fieldProperties.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(field, suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (fieldProperties.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(field, suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (fieldProperties.contains(FieldProperty.IDENTIFIER) && field != StandardField.PMID || field == StandardField.ISBN) {
            // Identifier editor does not support PMID, therefore excluded at the condition above
            return new IdentifierEditor(field, suggestionProvider, fieldCheckers);
        } else if (field == StandardField.ISSN) {
            return new ISSNEditor(field, suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (field == StandardField.OWNER) {
            return new OwnerEditor(field, suggestionProvider, fieldCheckers, undoAction, redoAction);
        } else if (field == StandardField.GROUPS) {
            return new GroupEditor(field, suggestionProvider, fieldCheckers, preferences, isMultiLine, undoManager, undoAction, redoAction);
        } else if (field == StandardField.FILE) {
            return new LinkedFilesEditor(field, databaseContext, suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(new YesNoEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
        } else if (fieldProperties.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(new
                    MonthEditorViewModel(field, suggestionProvider, databaseContext.getMode(), fieldCheckers, undoManager));
        } else if (fieldProperties.contains(FieldProperty.LANGUAGE)) {
            return new OptionEditor<>(new LanguageEditorViewModel(field, suggestionProvider, databaseContext.getMode(), fieldCheckers, undoManager));
        } else if (field == StandardField.GENDER) {
            return new OptionEditor<>(new GenderEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
        } else if (fieldProperties.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(new EditorTypeEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
        } else if (fieldProperties.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(new PaginationEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
        } else if (field == StandardField.TYPE) {
            if (entryType.equals(IEEETranEntryType.Patent)) {
                return new OptionEditor<>(new PatentTypeEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
            } else {
                return new OptionEditor<>(new TypeEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager));
            }
        } else if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldProperties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(field, databaseContext, suggestionProvider, fieldCheckers);
        } else if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonsEditor(field, suggestionProvider, fieldCheckers, isMultiLine, undoManager, undoAction, redoAction);
        } else if (StandardField.KEYWORDS == field) {
            return new KeywordsEditor(field, suggestionProvider, fieldCheckers);
        } else if (field == InternalField.KEY_FIELD) {
            return new CitationKeyEditor(field, suggestionProvider, fieldCheckers, databaseContext, undoAction, redoAction);
        } else if (fieldProperties.contains(FieldProperty.MARKDOWN)) {
            return new MarkdownEditor(field, suggestionProvider, fieldCheckers, preferences, undoManager, undoAction, redoAction);
        } else {
            // There was no specific editor found

            // Check whether there are selectors defined for the field at hand
            List<String> selectorValues = databaseContext.getMetaData().getContentSelectorValuesForField(field);
            if (!isMultiLine && !selectorValues.isEmpty()) {
                return new OptionEditor<>(new CustomFieldEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager, selectorValues));
            } else {
                return new SimpleEditor(field, suggestionProvider, fieldCheckers, preferences, isMultiLine, undoManager, undoAction, redoAction);
            }
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
