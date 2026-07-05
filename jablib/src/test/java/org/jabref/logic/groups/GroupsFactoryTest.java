package org.jabref.logic.groups;

import java.util.List;

import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupsFactoryTest {

    @Test
    void createRankParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createRankParentGroup();
        assertEquals("Rank", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertTrue(group.getIconName().isPresent());
    }

    @Test
    void createRankSubgroupsReturnsFiveEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createRankSubgroups();
        assertEquals(5, subgroups.size());
        for (SearchGroup sub : subgroups) {
            assertEquals(GroupHierarchyType.INDEPENDENT, sub.getHierarchicalContext());
            assertTrue(sub.getIconName().isPresent());
        }
    }

    @Test
    void createRelevanceParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createRelevanceParentGroup();
        assertEquals("Relevance", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createRelevanceSubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createRelevanceSubgroups().size());
    }

    @Test
    void createQualityParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createQualityParentGroup();
        assertEquals("Quality", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createQualitySubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createQualitySubgroups().size());
    }

    @Test
    void createPrintedParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createPrintedParentGroup();
        assertEquals("Printed", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createPrintedSubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createPrintedSubgroups().size());
    }

    @Test
    void createPriorityParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createPriorityParentGroup();
        assertEquals("Priority", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createPrioritySubgroupsReturnsThreeEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createPrioritySubgroups();
        assertEquals(3, subgroups.size());
        for (SearchGroup sub : subgroups) {
            assertTrue(sub.getIconName().isPresent());
        }
    }

    @Test
    void createReadStatusParentGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createReadStatusParentGroup();
        assertEquals("Read status", group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createReadStatusSubgroupsReturnsTwoEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createReadStatusSubgroups();
        assertEquals(2, subgroups.size());
        for (SearchGroup sub : subgroups) {
            assertTrue(sub.getIconName().isPresent());
        }
    }
}
