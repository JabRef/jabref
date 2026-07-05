package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

public class GroupsFactory {

    public static final String ALL_ENTRIES_GROUP_DEFAULT_ICON = "ALL_ENTRIES_GROUP_ICON";
    public static final String RANKING_ICON = "RANKING";
    public static final String RANK_1_ICON = "RANK1";
    public static final String RANK_2_ICON = "RANK2";
    public static final String RANK_3_ICON = "RANK3";
    public static final String RANK_4_ICON = "RANK4";
    public static final String RANK_5_ICON = "RANK5";
    public static final String RELEVANCE_ICON = "RELEVANCE";
    public static final String QUALITY_ICON = "QUALITY";
    public static final String PRINTED_ICON = "PRINTED";
    public static final String PRIORITY_ICON = "PRIORITY";
    public static final String PRIORITY_HIGH_ICON = "PRIORITY_HIGH";
    public static final String PRIORITY_MEDIUM_ICON = "PRIORITY_MEDIUM";
    public static final String PRIORITY_LOW_ICON = "PRIORITY_LOW";
    public static final String READ_STATUS_ICON = "READ_STATUS";
    public static final String READ_STATUS_READ_ICON = "READ_STATUS_READ";
    public static final String READ_STATUS_SKIMMED_ICON = "READ_STATUS_SKIMMED";

    private GroupsFactory() {
    }

    public static AllEntriesGroup createAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconName(ALL_ENTRIES_GROUP_DEFAULT_ICON);
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

    public static SearchGroup createRankParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Rank"),
                GroupHierarchyType.INCLUDING,
                "ranking =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(RANKING_ICON);
        return group;
    }

    public static List<SearchGroup> createRankSubgroups() {
        SearchGroup rank1 = new SearchGroup(Localization.lang("one"), GroupHierarchyType.INDEPENDENT, "ranking = rank1", EnumSet.noneOf(SearchFlags.class));
        rank1.setIconName(RANK_1_ICON);

        SearchGroup rank2 = new SearchGroup(Localization.lang("two"), GroupHierarchyType.INDEPENDENT, "ranking = rank2", EnumSet.noneOf(SearchFlags.class));
        rank2.setIconName(RANK_2_ICON);

        SearchGroup rank3 = new SearchGroup(Localization.lang("three"), GroupHierarchyType.INDEPENDENT, "ranking = rank3", EnumSet.noneOf(SearchFlags.class));
        rank3.setIconName(RANK_3_ICON);

        SearchGroup rank4 = new SearchGroup(Localization.lang("four"), GroupHierarchyType.INDEPENDENT, "ranking = rank4", EnumSet.noneOf(SearchFlags.class));
        rank4.setIconName(RANK_4_ICON);

        SearchGroup rank5 = new SearchGroup(Localization.lang("five"), GroupHierarchyType.INDEPENDENT, "ranking = rank5", EnumSet.noneOf(SearchFlags.class));
        rank5.setIconName(RANK_5_ICON);

        return List.of(rank1, rank2, rank3, rank4, rank5);
    }

    public static SearchGroup createRelevanceParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Relevance"),
                GroupHierarchyType.INCLUDING,
                "relevance =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(RELEVANCE_ICON);
        return group;
    }

    public static List<SearchGroup> createRelevanceSubgroups() {
        SearchGroup relevant = new SearchGroup(
                Localization.lang("Relevant"),
                GroupHierarchyType.INDEPENDENT,
                "relevance = relevant",
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notRelevant = new SearchGroup(
                Localization.lang("Not relevant"),
                GroupHierarchyType.INDEPENDENT,
                "relevance != relevant",
                EnumSet.noneOf(SearchFlags.class));

        return List.of(relevant, notRelevant);
    }

    public static SearchGroup createQualityParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Quality"),
                GroupHierarchyType.INCLUDING,
                "qualityassured =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(QUALITY_ICON);
        return group;
    }

    public static List<SearchGroup> createQualitySubgroups() {
        SearchGroup assured = new SearchGroup(
                Localization.lang("Assured"),
                GroupHierarchyType.INDEPENDENT,
                "qualityassured = qualityAssured",
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notAssured = new SearchGroup(
                Localization.lang("Not assured"),
                GroupHierarchyType.INDEPENDENT,
                "qualityassured != qualityAssured",
                EnumSet.noneOf(SearchFlags.class));

        return List.of(assured, notAssured);
    }

    public static SearchGroup createPrintedParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INCLUDING,
                "printed =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(PRINTED_ICON);
        return group;
    }

    public static List<SearchGroup> createPrintedSubgroups() {
        SearchGroup printed = new SearchGroup(
                Localization.lang("Printed"),
                GroupHierarchyType.INDEPENDENT,
                "printed = printed",
                EnumSet.noneOf(SearchFlags.class));

        SearchGroup notPrinted = new SearchGroup(
                Localization.lang("Not printed"),
                GroupHierarchyType.INDEPENDENT,
                "printed != printed",
                EnumSet.noneOf(SearchFlags.class));

        return List.of(printed, notPrinted);
    }

    public static SearchGroup createPriorityParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Priority"),
                GroupHierarchyType.INCLUDING,
                "priority =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(PRIORITY_ICON);
        return group;
    }

    public static List<SearchGroup> createPrioritySubgroups() {
        SearchGroup high = new SearchGroup(
                Localization.lang("high"),
                GroupHierarchyType.INDEPENDENT,
                "priority = prio1",
                EnumSet.noneOf(SearchFlags.class));
        high.setIconName(PRIORITY_HIGH_ICON);

        SearchGroup medium = new SearchGroup(
                Localization.lang("medium"),
                GroupHierarchyType.INDEPENDENT,
                "priority = prio2",
                EnumSet.noneOf(SearchFlags.class));
        medium.setIconName(PRIORITY_MEDIUM_ICON);

        SearchGroup low = new SearchGroup(
                Localization.lang("low"),
                GroupHierarchyType.INDEPENDENT,
                "priority = prio3",
                EnumSet.noneOf(SearchFlags.class));
        low.setIconName(PRIORITY_LOW_ICON);

        return List.of(high, medium, low);
    }

    public static SearchGroup createReadStatusParentGroup() {
        SearchGroup group = new SearchGroup(
                Localization.lang("Read status"),
                GroupHierarchyType.INCLUDING,
                "readstatus =~ .*",
                EnumSet.noneOf(SearchFlags.class));
        group.setIconName(READ_STATUS_ICON);
        return group;
    }

    public static List<SearchGroup> createReadStatusSubgroups() {
        SearchGroup read = new SearchGroup(
                Localization.lang("read"),
                GroupHierarchyType.INDEPENDENT,
                "readstatus = read",
                EnumSet.noneOf(SearchFlags.class));
        read.setIconName(READ_STATUS_READ_ICON);

        SearchGroup skimmed = new SearchGroup(
                Localization.lang("skimmed"),
                GroupHierarchyType.INDEPENDENT,
                "readstatus = skimmed",
                EnumSet.noneOf(SearchFlags.class));
        skimmed.setIconName(READ_STATUS_SKIMMED_ICON);

        return List.of(read, skimmed);
    }
}
