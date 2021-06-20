package org.jabref.gui.autocompleter;

import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SuggestionProvidersTest {

    private SuggestionProviders suggestionProviders;

    @BeforeEach
    public void initializeSuggestionProviders() {
        BibDatabase database = new BibDatabase();
        JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);
        Set<Field> completeFields = Set.of(StandardField.AUTHOR, StandardField.XREF, StandardField.XDATA, StandardField.JOURNAL, StandardField.PUBLISHER, SpecialField.PRINTED);
        AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(
                true,
                AutoCompleteFirstNameMode.BOTH,
                AutoCompletePreferences.NameFormat.BOTH,
                completeFields,
                null);
        this.suggestionProviders = new SuggestionProviders(database, abbreviationRepository, autoCompletePreferences);
    }

    private static Stream<Arguments> getTestPairs() {
        return Stream.of(
                // a person
                Arguments.of(org.jabref.gui.autocompleter.PersonNameSuggestionProvider.class, StandardField.AUTHOR),

                // a single entry field
                Arguments.of(org.jabref.gui.autocompleter.BibEntrySuggestionProvider.class, StandardField.XREF),

                // multi entry fieldg
                Arguments.of(org.jabref.gui.autocompleter.JournalsSuggestionProvider.class, StandardField.JOURNAL),

                // TODO: We should offer pre-configured publishers
                Arguments.of(org.jabref.gui.autocompleter.JournalsSuggestionProvider.class, StandardField.PUBLISHER),

                // TODO: Auto completion should be aware of possible values of special fields
                Arguments.of(org.jabref.gui.autocompleter.WordSuggestionProvider.class, SpecialField.PRINTED)
        );
    }

    @ParameterizedTest
    @MethodSource("getTestPairs")
    public void testAppropriateCompleterReturned(Class<SuggestionProvider<BibEntry>> expected, Field field) {
        assertEquals(expected, suggestionProviders.getForField(field).getClass());
    }

    @Test
    void emptySuggestionProviderReturnedForEmptySuggestionProviderList() {
        SuggestionProviders empty = new SuggestionProviders();
        assertEquals(EmptySuggestionProvider.class, empty.getForField(StandardField.AUTHOR).getClass());
    }
}
