package org.jabref.migrations;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvertLegacyExplicitGroupsTest {

    private PostOpenMigration action;
    private BibEntry entry;
    private ExplicitGroup group;

    @BeforeEach
    void setUp() {
        action = new ConvertLegacyExplicitGroups();

        entry = new BibEntry();
        entry.setCitationKey("Entry1");
        group = new ExplicitGroup("TestGroup", GroupHierarchyType.INCLUDING, ',');
        group.addLegacyEntryKey("Entry1");
    }

    @Test
    void performActionWritesGroupMembershipInEntry() {
        ParserResult parserResult = generateParserResult(GroupTreeNode.fromGroup(group));

        action.performMigration(parserResult);

        assertEquals(Optional.of("TestGroup"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void performActionClearsLegacyKeys() {
        ParserResult parserResult = generateParserResult(GroupTreeNode.fromGroup(group));

        action.performMigration(parserResult);

        assertEquals(List.of(), group.getLegacyEntryKeys());
    }

    @Test
    void performActionWritesGroupMembershipInEntryForComplexGroupTree() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        root.addSubgroup(new ExplicitGroup("TestGroup2", GroupHierarchyType.INCLUDING, ','));
        root.addSubgroup(group);
        ParserResult parserResult = generateParserResult(root);

        action.performMigration(parserResult);

        assertEquals(Optional.of("TestGroup"), entry.getField(StandardField.GROUPS));
    }

    private ParserResult generateParserResult(GroupTreeNode groupRoot) {
        ParserResult parserResult = new ParserResult(List.of(entry));
        parserResult.getMetaData().setGroups(groupRoot);
        return parserResult;
    }
}
