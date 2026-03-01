package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordsEditor extends TagsEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordsEditor.class);
    private static HashBiMap<String, String> mscmap;

    static {
        URL resourceUrl = KeywordsEditor.class.getClassLoader().getResource("msc_codes.json");

        if (resourceUrl == null) {
            LOGGER.error("Resource not found: msc_codes.json");
            mscmap = HashBiMap.create();
        }

        try {
            Optional<HashBiMap<String, String>> optionalMscCodes = MscCodeUtils.loadMscCodesFromJson(resourceUrl);

            if (optionalMscCodes.isPresent()) {
                mscmap = optionalMscCodes.get();
            } else {
                LOGGER.warn("Resource not found msc_codes.json");
                mscmap = HashBiMap.create();
            }
        } catch (MscCodeLoadingException e) {
            LOGGER.error("Error loading MSC codes", e);
            mscmap = HashBiMap.create();
        }
    }

    private final KeywordsEditorViewModel viewModel;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers) {

        super(field, suggestionProvider, fieldCheckers, Injector.instantiateModelOrService(UndoManager.class));

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                Injector.instantiateModelOrService(CliPreferences.class),
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
        if (mscmap.containsKey(tagLabel.getText())) {
            String mscClassification = mscmap.get(tagLabel.getText());
            Tooltip tooltip = new Tooltip(mscClassification);
            tagLabel.setTooltip(tooltip);
        }
    }

    public KeywordsEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }
}
