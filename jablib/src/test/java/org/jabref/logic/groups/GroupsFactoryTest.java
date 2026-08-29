package org.jabref.logic.groups;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@ResourceLock("Localization.lang")
class GroupsFactoryTest {

    private static String searchExpression(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " == " + value.getFieldValue().orElseThrow();
    }

    private static String searchExpressionNot(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " !== " + value.getFieldValue().orElseThrow();
    }

    @Test
    void createAllEntriesGroupHasCorrectProperties() {
        AllEntriesGroup group = GroupsFactory.createAllEntriesGroup();
        assertEquals(Localization.lang("All entries"), group.getName());
        assertEquals(GroupsFactory.GroupIcon.ALL_ENTRIES_GROUP_ICON.name(), group.getIconName().orElseThrow());
    }

    @Test
    void createWithoutFilesGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createWithoutFilesGroup();
        assertEquals(Localization.lang("Entries without linked files"), group.getName());
        assertEquals(GroupHierarchyType.INDEPENDENT, group.getHierarchicalContext());
        assertEquals("file !=~.*", group.getSearchExpression());
    }

    @Test
    void createWithoutGroupsGroupHasCorrectProperties() {
        SearchGroup group = GroupsFactory.createWithoutGroupsGroup();
        assertEquals(Localization.lang("Entries without groups"), group.getName());
        assertEquals(GroupHierarchyType.INDEPENDENT, group.getHierarchicalContext());
        assertEquals("groups !=~.*", group.getSearchExpression());
    }

    @Test
    void createRankParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createRankParentGroup();
        assertEquals(Localization.lang("Rank"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupIcon.RANKING.name(), group.getIconName().orElseThrow());
        assertEquals(GroupsFactory.GroupDescription.RANK.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> provideRankSubgroups() {
        return Stream.of(
                Arguments.of(0, "One", GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK1.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_1), GroupsFactory.GroupDescription.RANK_1.getDescription()),
                Arguments.of(1, "Two", GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK2.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_2), GroupsFactory.GroupDescription.RANK_2.getDescription()),
                Arguments.of(2, "Three", GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK3.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_3), GroupsFactory.GroupDescription.RANK_3.getDescription()),
                Arguments.of(3, "Four", GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK4.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_4), GroupsFactory.GroupDescription.RANK_4.getDescription()),
                Arguments.of(4, "Five", GroupHierarchyType.INDEPENDENT, GroupsFactory.GroupIcon.RANK5.name(), searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_5), GroupsFactory.GroupDescription.RANK_5.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("provideRankSubgroups")
    void createRankSubgroupsReturnsCorrectProperties(int index, String name, GroupHierarchyType hierarchyContext, String iconName, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createRankSubgroups();
        assertEquals(5, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(hierarchyContext, subgroup.getHierarchicalContext());
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createRelevanceParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createRelevanceParentGroup();
        assertEquals(Localization.lang("Relevance"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.RELEVANCE.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> provideRelevanceSubgroups() {
        return Stream.of(
                Arguments.of(0, "Relevant", searchExpression(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT), GroupsFactory.GroupDescription.RELEVANCE_RELEVANT.getDescription()),
                Arguments.of(1, "Not relevant", searchExpressionNot(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT), GroupsFactory.GroupDescription.RELEVANCE_NOT_RELEVANT.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("provideRelevanceSubgroups")
    void createRelevanceSubgroupsReturnsCorrectProperties(int index, String name, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createRelevanceSubgroups();
        assertEquals(2, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createQualityParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createQualityParentGroup();
        assertEquals(Localization.lang("Quality"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.QUALITY.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> provideQualitySubgroups() {
        return Stream.of(
                Arguments.of(0, "Assured", searchExpression(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED), GroupsFactory.GroupDescription.QUALITY_ASSURED.getDescription()),
                Arguments.of(1, "Not assured", searchExpressionNot(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED), GroupsFactory.GroupDescription.QUALITY_NOT_ASSURED.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("provideQualitySubgroups")
    void createQualitySubgroupsReturnsCorrectProperties(int index, String name, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createQualitySubgroups();
        assertEquals(2, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createPrintedParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createPrintedParentGroup();
        assertEquals(Localization.lang("Printed"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.PRINTED.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> providePrintedSubgroups() {
        return Stream.of(
                Arguments.of(0, "Printed", searchExpression(SpecialField.PRINTED, SpecialFieldValue.PRINTED), GroupsFactory.GroupDescription.PRINTED_PRINTED.getDescription()),
                Arguments.of(1, "Not printed", searchExpressionNot(SpecialField.PRINTED, SpecialFieldValue.PRINTED), GroupsFactory.GroupDescription.PRINTED_NOT_PRINTED.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("providePrintedSubgroups")
    void createPrintedSubgroupsReturnsCorrectProperties(int index, String name, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createPrintedSubgroups();
        assertEquals(2, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createPriorityParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createPriorityParentGroup();
        assertEquals(Localization.lang("Priority"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.PRIORITY.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> providePrioritySubgroups() {
        return Stream.of(
                Arguments.of(0, "High", GroupsFactory.GroupIcon.PRIORITY_HIGH.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_HIGH), GroupsFactory.GroupDescription.PRIORITY_HIGH.getDescription()),
                Arguments.of(1, "Medium", GroupsFactory.GroupIcon.PRIORITY_MEDIUM.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_MEDIUM), GroupsFactory.GroupDescription.PRIORITY_MEDIUM.getDescription()),
                Arguments.of(2, "Low", GroupsFactory.GroupIcon.PRIORITY_LOW.name(), searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_LOW), GroupsFactory.GroupDescription.PRIORITY_LOW.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("providePrioritySubgroups")
    void createPrioritySubgroupsReturnsCorrectProperties(int index, String name, String iconName, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createPrioritySubgroups();
        assertEquals(3, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createReadStatusParentGroupHasCorrectProperties() {
        ExplicitGroup group = GroupsFactory.createReadStatusParentGroup();
        assertEquals(Localization.lang("Read status"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.READ_STATUS.getDescription(), group.getDescription().orElseThrow());
    }

    static Stream<Arguments> provideReadStatusSubgroups() {
        return Stream.of(
                Arguments.of(0, "Read", GroupsFactory.GroupIcon.READ_STATUS_READ.name(), searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.READ), GroupsFactory.GroupDescription.READ_STATUS_READ.getDescription()),
                Arguments.of(1, "Skimmed", GroupsFactory.GroupIcon.READ_STATUS_SKIMMED.name(), searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.SKIMMED), GroupsFactory.GroupDescription.READ_STATUS_SKIMMED.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("provideReadStatusSubgroups")
    void createReadStatusSubgroupsReturnsCorrectProperties(int index, String name, String iconName, String searchExpression, String description) {
        List<SearchGroup> subgroups = GroupsFactory.createReadStatusSubgroups();
        assertEquals(2, subgroups.size());

        SearchGroup subgroup = subgroups.get(index);
        assertEquals(Localization.lang(name), subgroup.getName());
        assertEquals(iconName, subgroup.getIconName().orElseThrow());
        assertEquals(searchExpression, subgroup.getSearchExpression());
        assertEquals(description, subgroup.getDescription().orElseThrow());
    }

    @Test
    void createMarkingNodeHasCorrectPropertiesAndSubgroups() {
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        ExplicitGroup group = (ExplicitGroup) markingNode.getGroup();

        assertEquals(Localization.lang("Marking and Grading"), group.getName());
        assertEquals(GroupHierarchyType.INCLUDING, group.getHierarchicalContext());
        assertEquals(GroupsFactory.GroupDescription.MARKING_AND_GRADING.getDescription(), group.getDescription().orElseThrow());
        assertEquals(6, markingNode.getChildren().size());

        List<String> expectedSubgroupNames = List.of(
                Localization.lang("Rank"),
                Localization.lang("Relevance"),
                Localization.lang("Quality"),
                Localization.lang("Printed"),
                Localization.lang("Priority"),
                Localization.lang("Read status")
        );
        List<String> actualSubgroupNames = markingNode.getChildren().stream()
                                                      .map(node -> node.getGroup().getName())
                                                      .toList();
        assertEquals(expectedSubgroupNames, actualSubgroupNames);

        assertEquals(5, markingNode.getChildren().getFirst().getChildren().size());
        assertEquals(2, markingNode.getChildren().get(1).getChildren().size());
        assertEquals(2, markingNode.getChildren().get(2).getChildren().size());
        assertEquals(2, markingNode.getChildren().get(3).getChildren().size());
        assertEquals(3, markingNode.getChildren().get(4).getChildren().size());
        assertEquals(2, markingNode.getChildren().get(5).getChildren().size());
    }

    @Test
    void findMarkingNodeFindsNodeByDescription() {
        GroupTreeNode rootNode = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        rootNode.addChild(markingNode);

        assertEquals(markingNode, GroupsFactory.findMarkingNode(rootNode).orElseThrow());
    }

    @Test
    void hasAllSuggestedSubgroupsMatchesRegardlessOfGroupNames() {
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        markingNode.getGroup().nameProperty().setValue("Different Name");
        markingNode.getChildren().forEach(child -> child.getGroup().nameProperty().setValue("Renamed Parent"));

        assertTrue(GroupsFactory.hasAllSuggestedSubgroups(markingNode));
    }
}
