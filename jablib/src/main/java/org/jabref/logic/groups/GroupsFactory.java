package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class GroupsFactory {

    public enum GroupIcon {
        ALL_ENTRIES_GROUP_ICON,
        RANKING,
        RANK1,
        RANK2,
        RANK3,
        RANK4,
        RANK5,
        RELEVANCE,
        QUALITY,
        PRINTED,
        PRIORITY,
        PRIORITY_HIGH,
        PRIORITY_MEDIUM,
        PRIORITY_LOW,
        READ_STATUS,
        READ_STATUS_READ,
        READ_STATUS_SKIMMED
    }

    public enum GroupDescription {
        MARKING_AND_GRADING("default_marking_and_grading"),
        RANK("default_rank"),
        RANK_1("default_rank_1"),
        RANK_2("default_rank_2"),
        RANK_3("default_rank_3"),
        RANK_4("default_rank_4"),
        RANK_5("default_rank_5"),
        RELEVANCE("default_relevance"),
        RELEVANCE_RELEVANT("default_relevance_relevant"),
        RELEVANCE_NOT_RELEVANT("default_relevance_not_relevant"),
        QUALITY("default_quality"),
        QUALITY_ASSURED("default_quality_assured"),
        QUALITY_NOT_ASSURED("default_quality_not_assured"),
        PRINTED("default_printed"),
        PRINTED_PRINTED("default_printed_printed"),
        PRINTED_NOT_PRINTED("default_printed_not_printed"),
        PRIORITY("default_priority"),
        PRIORITY_HIGH("default_priority_high"),
        PRIORITY_MEDIUM("default_priority_medium"),
        PRIORITY_LOW("default_priority_low"),
        READ_STATUS("default_read_status"),
        READ_STATUS_READ("default_read_status_read"),
        READ_STATUS_SKIMMED("default_read_status_skimmed");

        private final String description;

        GroupDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public record SuggestedGroupStructure(ExplicitGroup parent, List<SearchGroup> subgroups) {
    }

    private GroupsFactory() {
    }

    public static AllEntriesGroup createAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconName(GroupIcon.ALL_ENTRIES_GROUP_ICON.name());
        return group;
    }

    public static SearchGroup createWithoutFilesGroup() {
        return new SearchGroup(
                Localization.lang("Entries without linked files"),
                GroupHierarchyType.INDEPENDENT,
                "file !=~.*",
                EnumSet.noneOf(SearchFlags.class));
    }

    public static SearchGroup createWithoutGroupsGroup() {
        return new SearchGroup(
                Localization.lang("Entries without groups"),
                GroupHierarchyType.INDEPENDENT,
                "groups !=~.*",
                EnumSet.noneOf(SearchFlags.class));
    }

    public static List<SuggestedGroupStructure> getSuggestedSubgroups() {
        return List.of(
                new SuggestedGroupStructure(createRankParentGroup(), createRankSubgroups()),
                new SuggestedGroupStructure(createRelevanceParentGroup(), createRelevanceSubgroups()),
                new SuggestedGroupStructure(createQualityParentGroup(), createQualitySubgroups()),
                new SuggestedGroupStructure(createPrintedParentGroup(), createPrintedSubgroups()),
                new SuggestedGroupStructure(createPriorityParentGroup(), createPrioritySubgroups()),
                new SuggestedGroupStructure(createReadStatusParentGroup(), createReadStatusSubgroups())
        );
    }

    public static GroupTreeNode createMarkingNode(Character keywordSeparator) {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Marking and Grading"),
                GroupHierarchyType.INCLUDING,
                keywordSeparator);
        group.setDescription(GroupDescription.MARKING_AND_GRADING.getDescription());
        GroupTreeNode markingNode = GroupTreeNode.fromGroup(group);

        for (SuggestedGroupStructure structure : getSuggestedSubgroups()) {
            addSuggestedSubgroup(markingNode, structure.parent(), structure.subgroups());
        }

        return markingNode;
    }

    public static ExplicitGroup createRankParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Rank"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.RANKING.name());
        group.setDescription(GroupDescription.RANK.getDescription());
        return group;
    }

    public static List<SearchGroup> createRankSubgroups() {
        return List.of(
                createRankSubgroup(Localization.lang("One"), SpecialFieldValue.RANK_1, GroupIcon.RANK1, GroupDescription.RANK_1),
                createRankSubgroup(Localization.lang("Two"), SpecialFieldValue.RANK_2, GroupIcon.RANK2, GroupDescription.RANK_2),
                createRankSubgroup(Localization.lang("Three"), SpecialFieldValue.RANK_3, GroupIcon.RANK3, GroupDescription.RANK_3),
                createRankSubgroup(Localization.lang("Four"), SpecialFieldValue.RANK_4, GroupIcon.RANK4, GroupDescription.RANK_4),
                createRankSubgroup(Localization.lang("Five"), SpecialFieldValue.RANK_5, GroupIcon.RANK5, GroupDescription.RANK_5));
    }

    private static SearchGroup createRankSubgroup(String name, SpecialFieldValue rank, GroupIcon icon, GroupDescription description) {
        SearchGroup group = new SearchGroup(
                name,
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.RANKING, rank),
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(icon.name());
        group.setDescription(description.getDescription());
        return group;
    }

    public static ExplicitGroup createRelevanceParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Relevance"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.RELEVANCE.name());
        group.setDescription(GroupDescription.RELEVANCE.getDescription());
        return group;
    }

    public static List<SearchGroup> createRelevanceSubgroups() {
        SearchGroup relevant = new SearchGroup(
                Localization.lang("Relevant"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT),
                EnumSet.noneOf(SearchFlags.class));
        relevant.setDescription(GroupDescription.RELEVANCE_RELEVANT.getDescription());

        SearchGroup notRelevant = new SearchGroup(
                Localization.lang("Not relevant"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT),
                EnumSet.noneOf(SearchFlags.class));
        notRelevant.setDescription(GroupDescription.RELEVANCE_NOT_RELEVANT.getDescription());

        return List.of(relevant, notRelevant);
    }

    public static ExplicitGroup createQualityParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Quality"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.QUALITY.name());
        group.setDescription(GroupDescription.QUALITY.getDescription());
        return group;
    }

    public static List<SearchGroup> createQualitySubgroups() {
        SearchGroup assured = new SearchGroup(
                Localization.lang("Assured"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED),
                EnumSet.noneOf(SearchFlags.class));
        assured.setDescription(GroupDescription.QUALITY_ASSURED.getDescription());

        SearchGroup notAssured = new SearchGroup(
                Localization.lang("Not assured"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED),
                EnumSet.noneOf(SearchFlags.class));
        notAssured.setDescription(GroupDescription.QUALITY_NOT_ASSURED.getDescription());

        return List.of(assured, notAssured);
    }

    public static ExplicitGroup createPrintedParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.PRINTED.name());
        group.setDescription(GroupDescription.PRINTED.getDescription());
        return group;
    }

    public static List<SearchGroup> createPrintedSubgroups() {
        SearchGroup printed = new SearchGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRINTED, SpecialFieldValue.PRINTED),
                EnumSet.noneOf(SearchFlags.class));
        printed.setDescription(GroupDescription.PRINTED_PRINTED.getDescription());

        SearchGroup notPrinted = new SearchGroup(
                Localization.lang("Not printed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.PRINTED, SpecialFieldValue.PRINTED),
                EnumSet.noneOf(SearchFlags.class));
        notPrinted.setDescription(GroupDescription.PRINTED_NOT_PRINTED.getDescription());

        return List.of(printed, notPrinted);
    }

    public static ExplicitGroup createPriorityParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Priority"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.PRIORITY.name());
        group.setDescription(GroupDescription.PRIORITY.getDescription());
        return group;
    }

    public static List<SearchGroup> createPrioritySubgroups() {
        SearchGroup high = new SearchGroup(
                Localization.lang("High"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_HIGH),
                EnumSet.noneOf(SearchFlags.class));
        high.setIconName(GroupIcon.PRIORITY_HIGH.name());
        high.setDescription(GroupDescription.PRIORITY_HIGH.getDescription());

        SearchGroup medium = new SearchGroup(
                Localization.lang("Medium"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_MEDIUM),
                EnumSet.noneOf(SearchFlags.class));
        medium.setIconName(GroupIcon.PRIORITY_MEDIUM.name());
        medium.setDescription(GroupDescription.PRIORITY_MEDIUM.getDescription());

        SearchGroup low = new SearchGroup(
                Localization.lang("Low"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_LOW),
                EnumSet.noneOf(SearchFlags.class));
        low.setIconName(GroupIcon.PRIORITY_LOW.name());
        low.setDescription(GroupDescription.PRIORITY_LOW.getDescription());

        return List.of(high, medium, low);
    }

    public static ExplicitGroup createReadStatusParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Read status"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.READ_STATUS.name());
        group.setDescription(GroupDescription.READ_STATUS.getDescription());
        return group;
    }

    public static List<SearchGroup> createReadStatusSubgroups() {
        SearchGroup read = new SearchGroup(
                Localization.lang("Read"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.READ),
                EnumSet.noneOf(SearchFlags.class));
        read.setIconName(GroupIcon.READ_STATUS_READ.name());
        read.setDescription(GroupDescription.READ_STATUS_READ.getDescription());

        SearchGroup skimmed = new SearchGroup(
                Localization.lang("Skimmed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.SKIMMED),
                EnumSet.noneOf(SearchFlags.class));
        skimmed.setIconName(GroupIcon.READ_STATUS_SKIMMED.name());
        skimmed.setDescription(GroupDescription.READ_STATUS_SKIMMED.getDescription());

        return List.of(read, skimmed);
    }

    private static String searchExpression(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " == " + value.getFieldValue().orElseThrow();
    }

    private static String searchExpressionNot(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " !== " + value.getFieldValue().orElseThrow();
    }

    private static void addSuggestedSubgroup(GroupTreeNode parentNode, AbstractGroup parentGroup, List<SearchGroup> subgroups) {
        GroupTreeNode childNode = parentNode.addSubgroup(parentGroup);
        subgroups.forEach(childNode::addSubgroup);
    }

    public static Optional<GroupTreeNode> findGroupByDescription(GroupTreeNode parentNode, GroupDescription description) {
        return findGroupByDescription(parentNode, description.getDescription());
    }

    public static Optional<GroupTreeNode> findGroupByDescription(GroupTreeNode parentNode, String description) {
        return parentNode.getChildren().stream()
                         .filter(child -> child.getGroup().getDescription().filter(description::equals).isPresent())
                         .findFirst();
    }

    public static Optional<GroupTreeNode> findMarkingNode(GroupTreeNode rootNode) {
        return findGroupByDescription(rootNode, GroupDescription.MARKING_AND_GRADING);
    }

    public static boolean hasAllSuggestedSubgroups(GroupTreeNode markingNode) {
        for (SuggestedGroupStructure structure : getSuggestedSubgroups()) {
            String parentDescription = structure.parent().getDescription().orElse("");
            Optional<GroupTreeNode> parentNodeOpt = findGroupByDescription(markingNode, parentDescription);
            if (parentNodeOpt.isEmpty()) {
                return false;
            }
            GroupTreeNode parentNode = parentNodeOpt.get();
            for (SearchGroup subgroup : structure.subgroups()) {
                String subgroupDescription = subgroup.getDescription().orElse("");
                Optional<GroupTreeNode> subgroupNodeOpt = findGroupByDescription(parentNode, subgroupDescription);
                if (subgroupNodeOpt.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
