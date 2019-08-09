package org.jabref.migrations;

import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvertMarkingToGroupsTest {
    @Test
    void performMigrationForSingleEntry() {
        BibEntry entry = new BibEntry()
                .withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]");
        ParserResult parserResult = new ParserResult(Collections.singleton(entry));

        new ConvertMarkingToGroups().performMigration(parserResult);

        GroupTreeNode rootExpected = GroupTreeNode.fromGroup(DefaultGroupsFactory.getAllEntriesGroup());
        GroupTreeNode markings = rootExpected.addSubgroup(new ExplicitGroup("Markings", GroupHierarchyType.INCLUDING, ','));
        markings.addSubgroup(new ExplicitGroup("Nicolas:6", GroupHierarchyType.INCLUDING, ','));

        assertEquals(Optional.empty(), entry.getField(InternalField.MARKED_INTERNAL));
        assertEquals(Optional.of(rootExpected), parserResult.getMetaData().getGroups());
    }
}
