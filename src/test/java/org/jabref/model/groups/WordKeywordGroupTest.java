package org.jabref.model.groups;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WordKeywordGroupTest {

    private WordKeywordGroup testGroup;
    private WordKeywordGroup testCaseSensitiveGroup;
    private WordKeywordGroup waterGroup;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        testGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, "keywords", "test", false, ',', false);
        testCaseSensitiveGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, "keywords", "test", true, ',', false);
        waterGroup = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, "keywords", "\\H2O", false, ',', false);
        entry = new BibEntry();
    }

    @Test
    public void containsFindsSameWord() {
        entry.setField("keywords", "test");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    public void containsFindsWordInSentence() throws Exception {
        entry.setField("keywords", "Some sentence containing test word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    public void containsFindsWordInCommaSeparatedList() throws Exception {
        entry.setField("keywords", "Some,list,containing,test,word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    public void containsFindsWordInSemicolonSeparatedList() throws Exception {
        entry.setField("keywords", "Some;list;containing;test;word");

        assertTrue(testGroup.contains(entry));
    }

    @Test
    public void containsFindsSameComplexWord() throws Exception {
        entry.setField("keywords", "\\H2O");

        assertTrue(waterGroup.contains(entry));
    }

    @Test
    public void containsFindsComplexWordInSentence() throws Exception {
        entry.setField("keywords", "Some sentence containing \\H2O word");

        assertTrue(waterGroup.contains(entry));
    }

    @Test
    public void containsDoesNotFindWordIfCaseDiffers() throws Exception {
        entry.setField("keywords", "Test");

        assertFalse(testCaseSensitiveGroup.contains(entry));
    }

    @Test
    public void containsDoesNotFindsWordInSentenceIfCaseDiffers() throws Exception {
        entry.setField("keywords", "Some sentence containing Test word");

        assertFalse(testCaseSensitiveGroup.contains(entry));
    }

    @Test
    public void addChangesFieldIfEmptyBefore() throws Exception {
        testGroup.add(entry);

        assertEquals(Optional.of("test"), entry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void addChangesFieldIfNotEmptyBefore() throws Exception {
        entry.setField(FieldName.KEYWORDS, "bla, blubb");
        testGroup.add(entry);

        assertEquals(Optional.of("bla, blubb, test"), entry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void addDoesNotAddDuplicate() throws Exception {
        entry.setField(FieldName.KEYWORDS, "test, blubb");
        testGroup.add(entry);

        assertEquals(Optional.of("test, blubb"), entry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void removeDoesNothingIfEntryNotMatched() throws Exception {
        entry.setField(FieldName.KEYWORDS, "something");
        testGroup.remove(entry);

        assertEquals(Optional.of("something"), entry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void removeRemovesNameFromField() throws Exception {
        entry.setField(FieldName.KEYWORDS, "test, blubb");
        testGroup.remove(entry);

        assertEquals(Optional.of("blubb"), entry.getField(FieldName.KEYWORDS));
    }
}
