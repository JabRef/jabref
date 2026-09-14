package org.jabref.gui.importer.actions;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LegacyGroupMembershipWarningActionTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final CliPreferences preferences = mock(CliPreferences.class);
    private final LegacyGroupMembershipWarningAction action = new LegacyGroupMembershipWarningAction();
    private final ParserResult parserResult = new ParserResult();
    private final GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
    private final ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');

    @Test
    void noWarningWithoutGroups() {
        assertFalse(action.isActionNecessary(parserResult, dialogService, preferences));
    }

    @Test
    void noWarningForCurrentStaticGroup() {
        root.addSubgroup(group);
        parserResult.getMetaData().setGroups(root);

        assertFalse(action.isActionNecessary(parserResult, dialogService, preferences));
    }

    // [utest->req~import.library.legacy-group-memberships-warned~1]
    @Test
    void warningForNestedGroupListingItsEntries() {
        group.addLegacyEntryKey("Smith2001");
        root.addSubgroup(new ExplicitGroup("parent", GroupHierarchyType.INDEPENDENT, ',')).addSubgroup(group);
        parserResult.getMetaData().setGroups(root);

        assertTrue(action.isActionNecessary(parserResult, dialogService, preferences));
    }
}
