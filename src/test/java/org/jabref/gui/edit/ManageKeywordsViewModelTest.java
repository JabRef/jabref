package org.jabref.gui.edit;

import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ManageKeywordsViewModelTest {

    private final PreferencesService preferences = mock(PreferencesService.class);
    private ManageKeywordsViewModel keywordsViewModel;

    @BeforeEach
    void setUp() {
        BibEntry entryOne = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Prakhar Srivastava and Nishant Singh")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/PARC49193.2020.236624")
                .withField(StandardField.ISBN, "978-1-7281-6575-2")
                .withField(StandardField.JOURNALTITLE, "2020 International Conference on Power Electronics & IoT Applications in Renewable Energy and its Control (PARC)")
                .withField(StandardField.PAGES, "351--354")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Automatized Medical Chatbot (Medibot)")
                .withField(StandardField.KEYWORDS, "Human-machine interaction, Chatbot, Medical Chatbot, Natural Language Processing, Machine Learning, Bot");

        BibEntry entryTwo = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Mladjan Jovanovic and Marcos Baez and Fabio Casati")
                .withField(StandardField.DATE, "November 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/MIC.2020.3037151")
                .withField(StandardField.ISSN, "1941-0131")
                .withField(StandardField.JOURNALTITLE, "IEEE Internet Computing")
                .withField(StandardField.PAGES, "1--1")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Chatbots as conversational healthcare services")
                .withField(StandardField.KEYWORDS, "Chatbot, Medical services, Internet, Data collection, Medical diagnostic imaging, Automation, Vocabulary");

        List<BibEntry> entries = List.of(entryOne, entryTwo);

        char delimiter = ',';
        when(preferences.getKeywordDelimiter()).thenReturn(delimiter);

        keywordsViewModel = new ManageKeywordsViewModel(preferences, entries);
    }

    @Test
    void keywordsFilledInCorrectly() {
        ObservableList<String> addedKeywords = keywordsViewModel.getKeywords();
        List<String> expectedKeywordsList = Arrays.asList("Human-machine interaction", "Chatbot", "Medical Chatbot",
                "Natural Language Processing", "Machine Learning", "Bot", "Chatbot", "Medical services", "Internet",
                "Data collection", "Medical diagnostic imaging", "Automation", "Vocabulary");

        assertEquals(FXCollections.observableList(expectedKeywordsList), addedKeywords);
    }

    @Test
    void removedKeywordNotIncludedInKeywordsList() {
        ObservableList<String> modifiedKeywords = keywordsViewModel.getKeywords();
        List<String> originalKeywordsList = Arrays.asList("Human-machine interaction", "Chatbot", "Medical Chatbot",
                "Natural Language Processing", "Machine Learning", "Bot", "Chatbot", "Medical services", "Internet",
                "Data collection", "Medical diagnostic imaging", "Automation", "Vocabulary");

        assertEquals(FXCollections.observableList(originalKeywordsList), modifiedKeywords, "compared lists are not identical");

        keywordsViewModel.removeKeyword("Human-machine interaction");

        assertNotEquals(FXCollections.observableList(originalKeywordsList), modifiedKeywords, "compared lists are identical");
    }
}
