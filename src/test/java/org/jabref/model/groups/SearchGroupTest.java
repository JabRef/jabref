package org.jabref.model.groups;

import java.util.EnumSet;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchGroupTest {

    private static BibEntry entry1D = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "Case")
            .withField(StandardField.GROUPS, "A");

    private static BibEntry entry2D = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry2")
            .withField(StandardField.AUTHOR, "TEST")
            .withField(StandardField.TITLE, "CASE")
            .withField(StandardField.GROUPS, "A");

    @Test
    void containsFindsWords() {
        SearchGroup groupPositive = new SearchGroup("A", GroupHierarchyType.INDEPENDENT, "Test", EnumSet.noneOf(SearchFlags.class));
        List<BibEntry> positiveResult = List.of(entry1D, entry2D);
        assertTrue(groupPositive.containsAll(positiveResult));
    }

    @Test
    void containsDoesNotFindWords() {
        SearchGroup groupNegative = new SearchGroup("A", GroupHierarchyType.INDEPENDENT, "Unknown", EnumSet.noneOf(SearchFlags.class));
        List<BibEntry> positiveResult = List.of(entry1D, entry2D);
        assertFalse(groupNegative.containsAny(positiveResult));
    }

    @Test
    void containsFindsWordWithRegularExpression() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "anyfield=rev*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        BibEntry entry = new BibEntry();
        entry.addKeyword("review", ',');

        assertTrue(group.contains(entry));
    }

    @Test
    void containsDoesNotFindsWordWithInvalidRegularExpression() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "anyfield=*rev*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        BibEntry entry = new BibEntry();
        entry.addKeyword("review", ',');

        assertFalse(group.contains(entry));
    }

    @Test
    void notQueryWorksWithLeftPartOfQuery() {
        SearchGroup groupToBeClassified = new SearchGroup("to-be-classified", GroupHierarchyType.INDEPENDENT, "NOT(groups=alpha) AND NOT(groups=beta)", EnumSet.noneOf(SearchFlags.class));

        BibEntry alphaEntry = new BibEntry()
                .withCitationKey("alpha")
                .withField(StandardField.GROUPS, "alpha");
        assertFalse(groupToBeClassified.contains(alphaEntry));
    }

    @Test
    void notQueryWorksWithLRightPartOfQuery() {
        SearchGroup groupToBeClassified = new SearchGroup("to-be-classified", GroupHierarchyType.INDEPENDENT, "NOT(groups=alpha) AND NOT(groups=beta)", EnumSet.noneOf(SearchFlags.class));

        BibEntry betaEntry = new BibEntry()
                .withCitationKey("beta")
                .withField(StandardField.GROUPS, "beta");
        assertFalse(groupToBeClassified.contains(betaEntry));
    }
}
