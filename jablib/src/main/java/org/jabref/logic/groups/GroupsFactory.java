package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

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

    public static ExplicitGroup createRankParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Rank"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.RANKING.name());
        return group;
    }

    public static List<SearchGroup> createRankSubgroups() {
        SearchGroup rank1 = new SearchGroup(Localization.lang("one"), GroupHierarchyType.INDEPENDENT, searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_1), EnumSet.noneOf(SearchFlags.class));
        rank1.setIconName(GroupIcon.RANK1.name());

        SearchGroup rank2 = new SearchGroup(Localization.lang("two"), GroupHierarchyType.INDEPENDENT, searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_2), EnumSet.noneOf(SearchFlags.class));
        rank2.setIconName(GroupIcon.RANK2.name());

        SearchGroup rank3 = new SearchGroup(Localization.lang("three"), GroupHierarchyType.INDEPENDENT, searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_3), EnumSet.noneOf(SearchFlags.class));
        rank3.setIconName(GroupIcon.RANK3.name());

        SearchGroup rank4 = new SearchGroup(Localization.lang("four"), GroupHierarchyType.INDEPENDENT, searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_4), EnumSet.noneOf(SearchFlags.class));
        rank4.setIconName(GroupIcon.RANK4.name());

        SearchGroup rank5 = new SearchGroup(Localization.lang("five"), GroupHierarchyType.INDEPENDENT, searchExpression(SpecialField.RANKING, SpecialFieldValue.RANK_5), EnumSet.noneOf(SearchFlags.class));
        rank5.setIconName(GroupIcon.RANK5.name());

        return List.of(rank1, rank2, rank3, rank4, rank5);
    }

    public static ExplicitGroup createRelevanceParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Relevance"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.RELEVANCE.name());
        return group;
    }

    public static List<SearchGroup> createRelevanceSubgroups() {
        SearchGroup relevant = new SearchGroup(
                Localization.lang("Relevant"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT),
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notRelevant = new SearchGroup(
                Localization.lang("Not relevant"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT),
                EnumSet.noneOf(SearchFlags.class));

        return List.of(relevant, notRelevant);
    }

    public static ExplicitGroup createQualityParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Quality"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.QUALITY.name());
        return group;
    }

    public static List<SearchGroup> createQualitySubgroups() {
        SearchGroup assured = new SearchGroup(
                Localization.lang("Assured"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED),
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notAssured = new SearchGroup(
                Localization.lang("Not assured"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED),
                EnumSet.noneOf(SearchFlags.class));

        return List.of(assured, notAssured);
    }

    public static ExplicitGroup createPrintedParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.PRINTED.name());
        return group;
    }

    public static List<SearchGroup> createPrintedSubgroups() {
        SearchGroup printed = new SearchGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRINTED, SpecialFieldValue.PRINTED),
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notPrinted = new SearchGroup(
                Localization.lang("Not printed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpressionNot(SpecialField.PRINTED, SpecialFieldValue.PRINTED),
                EnumSet.noneOf(SearchFlags.class));

        return List.of(printed, notPrinted);
    }

    public static ExplicitGroup createPriorityParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Priority"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.PRIORITY.name());
        return group;
    }

    public static List<SearchGroup> createPrioritySubgroups() {
        SearchGroup high = new SearchGroup(
                Localization.lang("high"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_HIGH),
                EnumSet.noneOf(SearchFlags.class));
        high.setIconName(GroupIcon.PRIORITY_HIGH.name());

        SearchGroup medium = new SearchGroup(
                Localization.lang("medium"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_MEDIUM),
                EnumSet.noneOf(SearchFlags.class));
        medium.setIconName(GroupIcon.PRIORITY_MEDIUM.name());

        SearchGroup low = new SearchGroup(
                Localization.lang("low"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_LOW),
                EnumSet.noneOf(SearchFlags.class));
        low.setIconName(GroupIcon.PRIORITY_LOW.name());

        return List.of(high, medium, low);
    }

    public static ExplicitGroup createReadStatusParentGroup() {
        ExplicitGroup group = new ExplicitGroup(
                Localization.lang("Read status"),
                GroupHierarchyType.INCLUDING,
                ',');
        group.setIconName(GroupIcon.READ_STATUS.name());
        return group;
    }

    public static List<SearchGroup> createReadStatusSubgroups() {
        SearchGroup read = new SearchGroup(
                Localization.lang("read"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.READ),
                EnumSet.noneOf(SearchFlags.class));
        read.setIconName(GroupIcon.READ_STATUS_READ.name());

        SearchGroup skimmed = new SearchGroup(
                Localization.lang("skimmed"),
                GroupHierarchyType.INDEPENDENT,
                searchExpression(SpecialField.READ_STATUS, SpecialFieldValue.SKIMMED),
                EnumSet.noneOf(SearchFlags.class));
        skimmed.setIconName(GroupIcon.READ_STATUS_SKIMMED.name());

        return List.of(read, skimmed);
    }

    private static String searchExpression(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " = " + value.getFieldValue().orElseThrow();
    }

    private static String searchExpressionNot(SpecialField field, SpecialFieldValue value) {
        return field.getName() + " != " + value.getFieldValue().orElseThrow();
    }
}
