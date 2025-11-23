package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.groups.GroupsFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MetaDataDiffTest {
    @Test
    void compareWithSameContentSelectorsDoesNotReportAnyDiffs() {
        MetaData one = new MetaData();
        one.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));
        MetaData two = new MetaData();
        two.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }

    @Test
    void defaultSettingEqualsEmptySetting() {
        MetaData one = new MetaData();
        // Field list is from {@link org.jabref.model.metadata.ContentSelectors.DEFAULT_FIELD_NAMES}
        one.addContentSelector(new ContentSelector(StandardField.AUTHOR, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.JOURNAL, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.PUBLISHER, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.KEYWORDS, List.of()));
        MetaData two = new MetaData();

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }

    @Test
    void allEntriesGroupIgnored() {
        MetaData one = new MetaData();
        one.setGroups(GroupTreeNode.fromGroup(GroupsFactory.getAllEntriesGroup()));
        MetaData two = new MetaData();

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }

    @Test
    void allEntriesGroupContainingGroupNotIgnored() {
        MetaData one = new MetaData();
        GroupTreeNode root = GroupTreeNode.fromGroup(GroupsFactory.getAllEntriesGroup());
        root.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        one.setGroups(root);

        MetaData two = new MetaData();

        assertNotEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }
}
