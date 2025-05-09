package org.jabref.model.groups;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordKeywordGroupTest {

    private WordKeywordGroup testGroup;
    private WordKeywordGroup testCaseSensitiveGroup;
    private WordKeywordGroup waterGroup;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        testGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "test", false, ',', false);
        testCaseSensitiveGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "test", true, ',', false);
        waterGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "\\H2O", false, ',', false);
        entry = new BibEntry();
    }

    @Test
    void containsFindsSameWord() {
        entry.setField(StandardField.KEYWORDS, "test");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    void containsFindsWordInSentence() {
        entry.setField(StandardField.KEYWORDS, "Some sentence containing test word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    void containsFindsWordInCommaSeparatedList() {
        entry.setField(StandardField.KEYWORDS, "Some,list,containing,test,word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    void containsFindsWordInSemicolonSeparatedList() {
        entry.setField(StandardField.KEYWORDS, "Some;list;containing;test;word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    void containsFindsSameComplexWord() {
        entry.setField(StandardField.KEYWORDS, "\\H2O");

        assertTrue(waterGroup.contains(entry));
    }

    @Test
    void containsFindsComplexWordInSentence() {
        entry.setField(StandardField.KEYWORDS, "Some sentence containing \\H2O word");

        assertTrue(waterGroup.contains(entry));
    }

    @Test
    void containsDoesNotFindWordIfCaseDiffers() {
        entry.setField(StandardField.KEYWORDS, "Test");

        assertFalse(testCaseSensitiveGroup.contains(entry));
    }

    @Test
    void containsDoesNotFindsWordInSentenceIfCaseDiffers() {
        entry.setField(StandardField.KEYWORDS, "Some sentence containing Test word");

        assertFalse(testCaseSensitiveGroup.contains(entry));
    }

    @Test
    void addChangesFieldIfEmptyBefore() {
        testGroup.add(entry);

        assertEquals(Optional.of("test"), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void addChangesFieldIfNotEmptyBefore() {
        entry.setField(StandardField.KEYWORDS, "bla, blubb");
        testGroup.add(entry);

        assertEquals(Optional.of("bla, blubb, test"), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void addDoesNotAddDuplicate() {
        entry.setField(StandardField.KEYWORDS, "test, blubb");
        testGroup.add(entry);

        assertEquals(Optional.of("test, blubb"), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void removeDoesNothingIfEntryNotMatched() {
        entry.setField(StandardField.KEYWORDS, "something");
        testGroup.remove(entry);

        assertEquals(Optional.of("something"), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void removeRemovesNameFromField() {
        entry.setField(StandardField.KEYWORDS, "test, blubb");
        testGroup.remove(entry);

        assertEquals(Optional.of("blubb"), entry.getField(StandardField.KEYWORDS));
    }
}
