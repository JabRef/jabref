package org.jabref.migrations;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
class ConvertSearchGroupSyntaxTest {

    private final ConvertSearchGroupSyntax migration = new ConvertSearchGroupSyntax(mock(DialogService.class));
    private final ParserResult parserResult = new ParserResult(Set.of(new BibEntry()));
    private final SearchGroup searchGroup = new SearchGroup("Data", GroupHierarchyType.INCLUDING, "author = harrer", EnumSet.of(SearchFlags.CASE_SENSITIVE, SearchFlags.REGULAR_EXPRESSION));

    @Test
    void flagsBecomeOperatorsAndVersionIsRecorded() {
        GroupTreeNode root = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        root.addSubgroup(searchGroup);
        parserResult.getMetaData().setGroups(root);
        parserResult.getMetaData().setContainsSearchGroups(true);
        assertTrue(migration.isMigrationNecessary(parserResult));

        migration.performMigration(parserResult);

        assertEquals("author =~! harrer", searchGroup.getSearchExpression());
        assertEquals(Optional.of(ConvertSearchGroupSyntax.VERSION_6_0_ALPHA_1), parserResult.getMetaData().getGroupSearchSyntaxVersion());
        assertFalse(migration.isMigrationNecessary(parserResult));
    }

    @Test
    void libraryWithoutSearchGroupsNeedsNoMigration() {
        assertFalse(migration.isMigrationNecessary(parserResult));
    }
}
