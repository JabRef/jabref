package org.jabref.model.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchGroupTest {
    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();
    private static final BibEntry ENTRY_1 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "Case")
            .withField(StandardField.GROUPS, "A");

    private static final BibEntry ENTRY_2 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry2")
            .withField(StandardField.AUTHOR, "TEST")
            .withField(StandardField.TITLE, "CASE")
            .withField(StandardField.GROUPS, "A");

    private final FilePreferences filePreferences = mock(FilePreferences.class);

    private static Stream<Arguments> testSearchGroup() {
        return Stream.of(
                // containsFindsWords
                Arguments.of("Test", List.of(ENTRY_1, ENTRY_2), true),
                // containsDoesNotFindWords
                Arguments.of("Unknown", List.of(ENTRY_1, ENTRY_2), false),
                // containsFindsWordWithRegularExpression
                Arguments.of("any:/rev*/", List.of(new BibEntry().withField(StandardField.KEYWORDS, "review")), true),
                // containsDoesNotFindsWordWithInvalidRegularExpression
                Arguments.of("any:/[rev/", List.of(new BibEntry().withField(StandardField.KEYWORDS, "review")), false),
                // notQueryWorksWithLeftPartOfQuery
                Arguments.of("(any:* AND -groups:alpha) AND (any:* AND -groups:beta)", List.of(new BibEntry().withField(StandardField.GROUPS, "alpha")), false),
                // notQueryWorksWithLRightPartOfQuery
                Arguments.of("(any:* AND -groups:alpha) AND (any:* AND -groups:beta)", List.of(new BibEntry().withField(StandardField.GROUPS, "beta")), false),
                // notQueryWorksWithBothPartsOfQuery
                Arguments.of("(any:* AND -groups:alpha) AND (any:* AND -groups:beta)", List.of(new BibEntry().withField(StandardField.GROUPS, "gamma")), true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSearchGroup(String searchTerm, List<BibEntry> entries, boolean expectedResult) {
        when(filePreferences.fulltextIndexLinkedFilesProperty()).thenReturn(mock(BooleanProperty.class));
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(false);

        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.getDatabase().insertEntries(entries);

        SearchGroup group = new SearchGroup("TestGroup", GroupHierarchyType.INDEPENDENT, searchTerm, EnumSet.noneOf(SearchFlags.class));
        assertEquals(expectedResult, group.containsAll(entries));
    }
}
