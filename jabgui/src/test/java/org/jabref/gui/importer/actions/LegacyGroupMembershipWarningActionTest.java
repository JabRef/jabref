package org.jabref.gui.importer.actions;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyGroupMembershipWarningActionTest {

    private final LegacyGroupMembershipWarningAction action = new LegacyGroupMembershipWarningAction();
    private final ParserResult parserResult = new ParserResult();
    private final GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
    private final ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');

    @Test
    void noWarningWithoutGroups() {
        assertFalse(action.isActionNecessary(parserResult, null, null));
    }

    @Test
    void noWarningForCurrentStaticGroup() {
        root.addSubgroup(group);
        parserResult.getMetaData().setGroups(root);

        assertFalse(action.isActionNecessary(parserResult, null, null));
    }

    @Test
    void warningForNestedGroupListingItsEntries() {
        group.addLegacyEntryKey("Smith2001");
        root.addSubgroup(new ExplicitGroup("parent", GroupHierarchyType.INDEPENDENT, ',')).addSubgroup(group);
        parserResult.getMetaData().setGroups(root);

        assertTrue(action.isActionNecessary(parserResult, null, null));
    }
}
