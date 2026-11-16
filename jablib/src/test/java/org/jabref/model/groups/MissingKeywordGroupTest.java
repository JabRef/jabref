package org.jabref.model.groups;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingKeywordGroupTest {

    private final MissingKeywordGroup group = new MissingKeywordGroup(GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',');

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "true  | ''",
            "true  | '   '",
            "true  | ', ,'",
            "false | A",
            "false | ', A'"
    })
    void containsMatchesOnlyEntriesWithoutKeyword(boolean expected, String keywords) {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, keywords);

        assertEquals(expected, group.contains(entry));
    }

    @Test
    void containsEntryWithoutTheField() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "A title");

        assertTrue(group.contains(entry));
    }
}
