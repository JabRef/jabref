package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordsEditor extends TagsEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordsEditor.class);
    private static final MscCodeRepository MSC_CODES = initializeRepository();

    private static MscCodeRepository initializeRepository() {
        URL resourceUrl = KeywordsEditor.class.getClassLoader().getResource("MSC_2020.csv");
        if (resourceUrl == null) {
            LOGGER.error("Resource not found: MSC_2020.csv");
            return new MscCodeRepository();
        }

        try {
            return MscCodeUtils.loadMscCodeRepositoryFromCsv(resourceUrl).orElseGet(MscCodeRepository::new);
        } catch (MscCodeLoadingException e) {
            LOGGER.error("Error loading MSC codes", e);
            return new MscCodeRepository();
        }
    }

    private final KeywordsEditorViewModel viewModel;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers,
                          CliPreferences preferences) {

        super(field, suggestionProvider, fieldCheckers, Injector.instantiateModelOrService(UndoManager.class));

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferences,
                undoManager);

        setupTagsField(
                KeywordsEditorViewModel.getStringConverter(),
                viewModel.getKeywordSeparator(),
                viewModel.keywordListProperty(),
                viewModel::getSuggestions);

        // Override the separator key handler to support escaped keywords
        String keywordSeparator = String.valueOf(viewModel.getKeywordSeparator());
        tagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(keywordSeparator)) {
                String editorText = tagsField.getEditor().getText();
                if (isSeparatedKeyword(editorText, keywordSeparator)) {
                    tagsField.commit();
                }
                event.consume();
            }
        });
    }

    private boolean isSeparatedKeyword(String keywordString, String keywordSeparator) {
        int separatorLastOccurrence = keywordString.lastIndexOf(keywordSeparator);
        if (separatorLastOccurrence == -1) {
            return false;
        }

        int separatorFirstOccurrence = keywordString.lastIndexOf(keywordSeparator);
        String substringWithSeparator = new StringBuilder(keywordString.substring(0, separatorFirstOccurrence)).reverse().toString();

        AtomicBoolean isSeparatedKeyword = new AtomicBoolean(true);
        substringWithSeparator.chars().takeWhile(symbol -> symbol == Keyword.DEFAULT_ESCAPE_SYMBOL)
                              .forEachOrdered(_ -> isSeparatedKeyword.set(!isSeparatedKeyword.get()));

        return isSeparatedKeyword.get();
    }

    @Override
    protected void customizeTag(Label tagLabel, Keyword keyword) {
        MSC_CODES.getDescription(tagLabel.getText())
                 .ifPresent(mscClassification -> tagLabel.setTooltip(new Tooltip(mscClassification)));
    }

    public KeywordsEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }
}
