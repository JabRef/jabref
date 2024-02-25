package org.jabref.gui.fieldeditors;

import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AppendWordsStrategy;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class KeywordsEditorViewModel extends AbstractEditorViewModel {

    private final PreferencesService preferencesService;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final DialogService dialogService;
    private final Character keywordSeparator;
    private final KeywordList contentSelectorKeywords;
    private final ObservableList<Keyword> filteredKeywords;

    public KeywordsEditorViewModel(Field field,
                                   SuggestionProvider<?> suggestionProvider,
                                   FieldCheckers fieldCheckers,
                                   PreferencesService preferencesService,
                                   BibDatabaseContext databaseContext,
                                   UndoManager undoManager,
                                   DialogService dialogService) {

        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.preferencesService = preferencesService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.dialogService = dialogService;

        keywordSeparator = preferencesService.getBibEntryPreferences().getKeywordSeparator();

        filteredKeywords = FXCollections.observableArrayList();
        contentSelectorKeywords = new KeywordList(databaseContext.getMetaData().getContentSelectorValuesForField(StandardField.KEYWORDS));

        textProperty().addListener((observable, oldValue, newValue) -> updateFilteredKeywords(newValue));
    }

    public AutoCompletionStrategy getAutoCompletionStrategy() {
        return new AppendWordsStrategy();
    }

    public ObservableList<Keyword> getFilteredKeywords() {
        return filteredKeywords;
    }

    private void updateFilteredKeywords(String existingKeywords) {
        KeywordList parsedKeywords = KeywordList.parse(existingKeywords, keywordSeparator);
        filteredKeywords.setAll(contentSelectorKeywords.stream()
                                                       .filter(keyword -> !parsedKeywords.contains(keyword))
                                                       .collect(Collectors.toList()));
    }

    public void addKeyword(Keyword selectedItem) {
        if (selectedItem != null) {
            KeywordList currentKeywords = KeywordList.parse(textProperty().getValue(), keywordSeparator);
            currentKeywords.add(selectedItem);
            Platform.runLater(() -> textProperty().setValue(currentKeywords.getAsString(keywordSeparator)));
        }
    }
}
