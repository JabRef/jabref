package org.jabref.logic.groups;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ResourceLock("Localization.lang")
class GroupsFactoryTest {

    private static String searchExpression(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " = " + value.getFieldValue().orElseThrow();
    }

    @Test
    void createRankParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createRankParentGroup();
        assertEquals(Localization.lang("Rank"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupIcon.RANKING.name(), group.getIconName().orElseThrow());
    }

    static Stream<Arguments> provideRankSubgroups() {
        return Stream.of(
                Arguments.of(0, GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK1.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_1)),
                Arguments.of(1, GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK2.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_2)),
                Arguments.of(2, GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK3.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_3)),
                Arguments.of(3, GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK4.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_4)),
                Arguments.of(4, GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK5.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_5))
        );
    }

    @ParameterizedTest
    @MethodSource("provideRankSubgroups")
    void createRankSubgroupsReturnsCorrectProperties(int index, GroupHierarchyType hierarchyContext, String iconName, String searchExpression) {
        List<SearchGroup> subgroups = GroupsFactory.createRankSubgroups();
        assertEquals(5, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(hierarchyContext, subgroup.getHierarchicalContext());
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
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

    static Stream<Arguments> providePrioritySubgroups() {
        return Stream.of(
                Arguments.of(0, GroupsFactory.GroupIcon.PRIORITY_HIGH.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_HIGH)),
                Arguments.of(1, GroupsFactory.GroupIcon.PRIORITY_MEDIUM.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_MEDIUM)),
                Arguments.of(2, GroupsFactory.GroupIcon.PRIORITY_LOW.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_LOW))
        );
    }

    @ParameterizedTest
    @MethodSource("providePrioritySubgroups")
    void createPrioritySubgroupsReturnsCorrectProperties(int index, String iconName, String searchExpression) {
        List<SearchGroup> subgroups = GroupsFactory.createPrioritySubgroups();
        assertEquals(3, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
    }

    @Test
    void createReadStatusParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createReadStatusParentGroup();
        assertEquals(Localization.lang("Read status"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
    }

    static Stream<Arguments> provideReadStatusSubgroups() {
        return Stream.of(
                Arguments.of(0, GroupsFactory.GroupIcon.READ_STATUS_READ.name(), searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.READ)),
                Arguments.of(1, GroupsFactory.GroupIcon.READ_STATUS_SKIMMED.name(), searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.SKIMMED))
        );
    }

    @ParameterizedTest
    @MethodSource("provideReadStatusSubgroups")
    void createReadStatusSubgroupsReturnsCorrectProperties(int index, String iconName, String searchExpression) {
        List<SearchGroup> subgroups = GroupsFactory.createReadStatusSubgroups();
        assertEquals(2, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
    }
}
