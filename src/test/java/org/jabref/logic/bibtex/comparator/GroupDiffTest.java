package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupDiffTest {

    private final MetaData originalMetaData = mock(MetaData.class);
    private final MetaData newMetaData = mock(MetaData.class);
    private GroupTreeNode rootOriginal;

    @BeforeEach
    void setup() {
        rootOriginal = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        rootOriginal.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode parent = rootOriginal
                .addSubgroup(new ExplicitGroup("ExplicitParent", GroupHierarchyType.INDEPENDENT, ','));
        parent.addSubgroup(new ExplicitGroup("ExplicitNode", GroupHierarchyType.REFINING, ','));
    }

    @Test
    void compareEmptyGroups() {
        when(originalMetaData.getGroups()).thenReturn(Optional.empty());
        when(newMetaData.getGroups()).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), GroupDiff.compare(originalMetaData, newMetaData));
    }

    @Test
    void compareGroupWithItself() {
        when(originalMetaData.getGroups()).thenReturn(Optional.of(rootOriginal));
        when(newMetaData.getGroups()).thenReturn(Optional.of(rootOriginal));

        assertEquals(Optional.empty(), GroupDiff.compare(originalMetaData, newMetaData));
    }

    @Test
    void compareWithChangedGroup() {
        GroupTreeNode rootModified = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        rootModified.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));

        when(originalMetaData.getGroups()).thenReturn(Optional.of(rootOriginal));
        when(newMetaData.getGroups()).thenReturn(Optional.of(rootModified));

        Optional<GroupDiff> groupDiff = GroupDiff.compare(originalMetaData, newMetaData);

        Optional<GroupDiff> expectedGroupDiff = Optional.of(new GroupDiff(newMetaData.getGroups().get(), originalMetaData.getGroups().get()));

        assertEquals(expectedGroupDiff.get().getNewGroupRoot(), groupDiff.get().getNewGroupRoot());
        assertEquals(expectedGroupDiff.get().getOriginalGroupRoot(), groupDiff.get().getOriginalGroupRoot());
    }

}
