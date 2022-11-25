package org.jabref.model.groups;

import java.util.EnumSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.rules.SearchRules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchGroupTest {

    @Test
    public void containsFindsWordWithRegularExpression() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "anyfield=rev*", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        BibEntry entry = new BibEntry();
        entry.addKeyword("review", ',');

        assertTrue(group.contains(entry));
    }

    @Test
    public void containsDoesNotFindsWordWithInvalidRegularExpression() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "anyfield=*rev*", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        BibEntry entry = new BibEntry();
        entry.addKeyword("review", ',');

        assertFalse(group.contains(entry));
    }

    @Test
    public void notQueryWorksWithLeftPartOfQuery() {
        SearchGroup groupToBeClassified = new SearchGroup("to-be-classified", GroupHierarchyType.INDEPENDENT, "NOT(groups=alpha) AND NOT(groups=beta)", EnumSet.noneOf(SearchRules.SearchFlags.class));

        BibEntry alphaEntry = new BibEntry()
                .withCitationKey("alpha")
                .withField(StandardField.GROUPS, "alpha");
        assertFalse(groupToBeClassified.contains(alphaEntry));
    }

    @Test
    public void notQueryWorksWithLRightPartOfQuery() {
        SearchGroup groupToBeClassified = new SearchGroup("to-be-classified", GroupHierarchyType.INDEPENDENT, "NOT(groups=alpha) AND NOT(groups=beta)", EnumSet.noneOf(SearchRules.SearchFlags.class));

        BibEntry betaEntry = new BibEntry()
                .withCitationKey("beta")
                .withField(StandardField.GROUPS, "beta");
        assertFalse(groupToBeClassified.contains(betaEntry));
    }
}
