package org.jabref.logic.groups;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupsHelperTest {

    private static final Character KEYWORD_SEPARATOR = ',';

    private BibEntry entry(String citationKey) {
        return new BibEntry(StandardEntryType.Article).withCitationKey(citationKey);
    }

    private GroupTreeNode groupNamed(BibDatabaseContext context, String name) {
        return context.getMetaData().getGroups()
                      .orElseThrow(() -> new AssertionError("No group tree created"))
                      .getChildren().stream()
                      .filter(node -> name.equals(node.getName()))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("Group '" + name + "' not found"));
    }

    @Test
    void assignCreatesTopLevelGroupAndAssignsEntries() {
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry first = entry("first");
        BibEntry second = entry("second");

        GroupsHelper.assignEntriesToGroup(context, List.of(first, second), "assignCreatesTopLevelGroupAndAssignsEntries", KEYWORD_SEPARATOR);

        GroupTreeNode created = groupNamed(context, "assignCreatesTopLevelGroupAndAssignsEntries");
        assertInstanceOf(ExplicitGroup.class, created.getGroup());
        assertTrue(created.getGroup().contains(first));
        assertTrue(created.getGroup().contains(second));
    }

    @Test
    void assignReusesExistingGroupInsteadOfCreatingDuplicate() {
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry first = entry("first");
        BibEntry second = entry("second");

        GroupsHelper.assignEntriesToGroup(context, List.of(first), "Shared", KEYWORD_SEPARATOR);
        GroupsHelper.assignEntriesToGroup(context, List.of(second), "Shared", KEYWORD_SEPARATOR);

        long sharedGroups = context.getMetaData().getGroups().orElseThrow()
                                   .getChildren().stream()
                                   .filter(node -> "Shared".equals(node.getName()))
                                   .count();
        assertEquals(1, sharedGroups);

        GroupTreeNode shared = groupNamed(context, "Shared");
        assertTrue(shared.getGroup().contains(first));
        assertTrue(shared.getGroup().contains(second));
    }
}
