package org.jabref.migrations;

import java.util.Optional;
import java.util.Set;

import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertMarkingToGroupsTest {

    @Test
    void everyMarkingTokenAndLeftoverTextBecomeGroups() {
        BibEntry entry = new BibEntry()
                .withField(InternalField.MARKED_INTERNAL, "note [Alice:1][Bob:2]");
        ParserResult parserResult = new ParserResult(Set.of(entry));

        new ConvertMarkingToGroups().performMigration(parserResult);

        GroupTreeNode rootExpected = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        GroupTreeNode markings = rootExpected.addSubgroup(new ExplicitGroup("Markings", GroupHierarchyType.INCLUDING, ','));
        markings.addSubgroup(new ExplicitGroup("Alice:1", GroupHierarchyType.INCLUDING, ','));
        markings.addSubgroup(new ExplicitGroup("Bob:2", GroupHierarchyType.INCLUDING, ','));
        markings.addSubgroup(new ExplicitGroup("note", GroupHierarchyType.INCLUDING, ','));
        assertEquals(Optional.of(rootExpected), parserResult.getMetaData().getGroups());
    }

    @Test
    void performMigrationForSingleEntry() {
        BibEntry entry = new BibEntry()
                .withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]");
        ParserResult parserResult = new ParserResult(Set.of(entry));

        ConvertMarkingToGroups migration = new ConvertMarkingToGroups();
        assertTrue(migration.isMigrationNecessary(parserResult));

        migration.performMigration(parserResult);

        GroupTreeNode rootExpected = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        GroupTreeNode markings = rootExpected.addSubgroup(new ExplicitGroup("Markings", GroupHierarchyType.INCLUDING, ','));
        markings.addSubgroup(new ExplicitGroup("Nicolas:6", GroupHierarchyType.INCLUDING, ','));

        assertEquals(Optional.empty(), entry.getField(InternalField.MARKED_INTERNAL));
        assertEquals(Optional.of(rootExpected), parserResult.getMetaData().getGroups());
        assertFalse(migration.isMigrationNecessary(parserResult));
    }
}
