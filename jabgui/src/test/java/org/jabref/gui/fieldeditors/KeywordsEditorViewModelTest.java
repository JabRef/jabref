package org.jabref.gui.fieldeditors;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeywordsEditorViewModelTest {
    private KeywordsEditorViewModel viewModel;

    @BeforeEach
    void setUp() {
        SuggestionProvider<String> suggestionProvider = mock(SuggestionProvider.class);
        CliPreferences cliPreferences = mock(CliPreferences.class);

        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);

        when(cliPreferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        when(suggestionProvider.getPossibleSuggestions()).thenReturn(List.of("value", "key\\,\\\\", "parent > node > child", "father \\> inheritor"));
        viewModel = new KeywordsEditorViewModel(mock(Field.class), suggestionProvider, mock(FieldCheckers.class), cliPreferences, mock(UndoManager.class));
    }

    @Test
    void getSuggestionsWithEscapedSeparator() {
        String request = "key";
        assertEquals(List.of(new Keyword(request), new Keyword("key\\,\\\\")), viewModel.getSuggestions(request));
    }

    @Test
    void getSuggestionsWithEscapedHierarchicalDelimiter() {
        String request = "father";
        assertEquals(List.of(new Keyword(request), new Keyword("father \\> inheritor")), viewModel.getSuggestions(request));
    }

    @Test
    void parseKeywordWithHierarchicalKeywords() {
        String hierarchichalString = "parent > node > child";
        Keyword parsedKeyword = KeywordList.parse(hierarchichalString, viewModel.getKeywordSeparator()).get(0);

        assertEquals(parsedKeyword, viewModel.parseKeyword(hierarchichalString));
    }

    @Test
    void parseKeywordWithMultipleKeywords() {
        String multipleKeysStr = "key1, key2";
        Keyword firstParsedKeyword = KeywordList.parse(multipleKeysStr, viewModel.getKeywordSeparator()).get(0);

        assertEquals(firstParsedKeyword, viewModel.parseKeyword(multipleKeysStr));
    }

    @Test
    void stringConverterToStringWithHierarchicalKeywords() {
        String hierarchichalString = "parent > node > child";
        Keyword keyword = Keyword.ofHierarchical(hierarchichalString);

        assertEquals(hierarchichalString, KeywordsEditorViewModel.getStringConverter().toString(keyword));
    }
}
