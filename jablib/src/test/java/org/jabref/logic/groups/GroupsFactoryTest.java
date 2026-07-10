package org.jabref.logic.groups;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ResourceLock("Localization.lang")
class GroupsFactoryTest {

    @Test
    void createRankParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createRankParentGroup();
        assertEquals(Localization.lang("Rank"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.RANKING_ICON, group.getIconName().orElseThrow());
    }

    @Test
    void createRankSubgroupsReturnsFiveEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createRankSubgroups();
        assertEquals(5, subgroups.size());

        assertEquals(GroupHierarchyType.INDEPENDENT, subgroups.getFirst().getHierarchicalContext());
        assertEquals(GroupsFactory.RANK_1_ICON, subgroups.getFirst().getIconName().orElseThrow());
        assertEquals("ranking = rank1", subgroups.getFirst().getSearchExpression());

        assertEquals(GroupHierarchyType.INDEPENDENT, subgroups.get(1).getHierarchicalContext());
        assertEquals(GroupsFactory.RANK_2_ICON, subgroups.get(1).getIconName().orElseThrow());
        assertEquals("ranking = rank2", subgroups.get(1).getSearchExpression());

        assertEquals(GroupHierarchyType.INDEPENDENT, subgroups.get(2).getHierarchicalContext());
        assertEquals(GroupsFactory.RANK_3_ICON, subgroups.get(2).getIconName().orElseThrow());
        assertEquals("ranking = rank3", subgroups.get(2).getSearchExpression());

        assertEquals(GroupHierarchyType.INDEPENDENT, subgroups.get(3).getHierarchicalContext());
        assertEquals(GroupsFactory.RANK_4_ICON, subgroups.get(3).getIconName().orElseThrow());
        assertEquals("ranking = rank4", subgroups.get(3).getSearchExpression());

        assertEquals(GroupHierarchyType.INDEPENDENT, subgroups.get(4).getHierarchicalContext());
        assertEquals(GroupsFactory.RANK_5_ICON, subgroups.get(4).getIconName().orElseThrow());
        assertEquals("ranking = rank5", subgroups.get(4).getSearchExpression());
    }

    @Test
    void createRelevanceParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createRelevanceParentGroup();
        assertEquals(Localization.lang("Relevance"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createRelevanceSubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createRelevanceSubgroups().size());
    }

    @Test
    void createQualityParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createQualityParentGroup();
        assertEquals(Localization.lang("Quality"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createQualitySubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createQualitySubgroups().size());
    }

    @Test
    void createPrintedParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createPrintedParentGroup();
        assertEquals(Localization.lang("Printed"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createPrintedSubgroupsReturnsTwoEntries() {
        assertEquals(2, GroupsFactory.createPrintedSubgroups().size());
    }

    @Test
    void createPriorityParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createPriorityParentGroup();
        assertEquals(Localization.lang("Priority"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createPrioritySubgroupsReturnsThreeEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createPrioritySubgroups();
        assertEquals(3, subgroups.size());

        assertEquals(GroupsFactory.PRIORITY_HIGH_ICON, subgroups.getFirst().getIconName().orElseThrow());
        assertEquals("priority = prio1", subgroups.getFirst().getSearchExpression());

        assertEquals(GroupsFactory.PRIORITY_MEDIUM_ICON, subgroups.get(1).getIconName().orElseThrow());
        assertEquals("priority = prio2", subgroups.get(1).getSearchExpression());

        assertEquals(GroupsFactory.PRIORITY_LOW_ICON, subgroups.get(2).getIconName().orElseThrow());
        assertEquals("priority = prio3", subgroups.get(2).getSearchExpression());
    }

    @Test
    void createReadStatusParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createReadStatusParentGroup();
        assertEquals(Localization.lang("Read status"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    @Test
    void createReadStatusSubgroupsReturnsTwoEntries() {
        List<SearchGroup> subgroups = GroupsFactory.createReadStatusSubgroups();
        assertEquals(2, subgroups.size());

        assertEquals(GroupsFactory.READ_STATUS_READ_ICON, subgroups.getFirst().getIconName().orElseThrow());
        assertEquals("readstatus = read", subgroups.getFirst().getSearchExpression());

        assertEquals(GroupsFactory.READ_STATUS_SKIMMED_ICON, subgroups.get(1).getIconName().orElseThrow());
        assertEquals("readstatus = skimmed", subgroups.get(1).getSearchExpression());
    }
}
