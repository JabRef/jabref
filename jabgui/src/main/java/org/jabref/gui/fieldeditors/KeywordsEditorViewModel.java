package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;

import org.tinylog.Logger;

public class KeywordsEditorViewModel extends AbstractEditorViewModel {

    private final ListProperty<Keyword> keywordListProperty;
    private final Character keywordSeparator;
    private final SuggestionProvider<?> suggestionProvider;

    public KeywordsEditorViewModel(Field field,
                                   SuggestionProvider<?> suggestionProvider,
                                   FieldCheckers fieldCheckers,
                                   CliPreferences preferences,
                                   UndoManager undoManager) {

        super(field, suggestionProvider, fieldCheckers, undoManager);

        keywordListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.keywordSeparator = preferences.getBibEntryPreferences().getKeywordSeparator();
        this.suggestionProvider = suggestionProvider;

        BindingsHelper.bindContentBidirectional(
                keywordListProperty,
                text,
                this::serializeKeywords,
                this::parseKeywords);
    }

    private String serializeKeywords(List<Keyword> keywords) {
        return KeywordList.serialize(keywords, keywordSeparator);
    }

    private List<Keyword> parseKeywords(String newText) {
        return KeywordList.parse(newText, keywordSeparator).stream().toList();
    }

    public ListProperty<Keyword> keywordListProperty() {
        return keywordListProperty;
    }

    public StringConverter<Keyword> getStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Keyword keyword) {
                if (keyword == null) {
                    Logger.debug("Keyword is null");
                    return "";
                }
                return keyword.get();
            }

            @Override
            public Keyword fromString(String keywordString) {
                return new Keyword(keywordString);
            }
        };
    }

    public List<Keyword> getSuggestions(String request) {
        List<Keyword> suggestions = suggestionProvider.getPossibleSuggestions().stream()
                                                      .map(String.class::cast)
                                                      .filter(keyword -> keyword.toLowerCase().contains(request.toLowerCase()))
                                                      .map(Keyword::new)
                                                      .distinct()
                                                      .collect(Collectors.toList());

        Keyword requestedKeyword = new Keyword(request);
        if (!suggestions.contains(requestedKeyword)) {
            suggestions.addFirst(requestedKeyword);
        }

        return suggestions;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }
}
