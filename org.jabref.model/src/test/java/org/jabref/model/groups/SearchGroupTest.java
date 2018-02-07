package org.jabref.model.groups;

import org.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SearchGroupTest {


    @Test
    public void containsFindsWordWithRegularExpression() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "anyfield=rev*", true, true);
        BibEntry entry = new BibEntry();
        entry.addKeyword("review", ',');

        assertTrue(group.contains(entry));
    }
}
