package org.jabref.gui.groups;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.groups.SuggestedGroups;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The suggested groups are created in jablib, which stores icon names as text.
/// Only the GUI resolves them, so a typo would not fail the compilation but show a default icon.
@NullMarked
class SuggestedGroupsIconTest {

    @Test
    void everyIconNameIsAnIconOfJabRef() {
        GroupTreeNode allEntries = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        SuggestedGroups.addMissing(allEntries);

        List<String> unknownIcons = allEntries.iterateOverTree()
                                              .map(node -> node.getGroup().getIconName())
                                              .flatMap(Optional::stream)
                                              .filter(iconName -> IconTheme.findGroupIcon(iconName).isEmpty())
                                              .toList();

        assertEquals(List.of(), unknownIcons);
    }
}
