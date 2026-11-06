package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

public class GroupsFactory {

    public static final String ALL_ENTRIES_GROUP_DEFAULT_ICON = "ALL_ENTRIES_GROUP_ICON";

    /// The special fields in the order of the "Marking and grading" groups
    private static final List<SpecialField> MARKING_FIELDS = List.of(
            SpecialField.RANKING,
            SpecialField.RELEVANCE,
            SpecialField.QUALITY,
            SpecialField.PRINTED,
            SpecialField.PRIORITY,
            SpecialField.READ_STATUS);

    // The colors of the flag and eye icons in the entry table, because the group tree paints an icon in the color of its group
    private static final String RED = "#ff0000";
    private static final String ORANGE = "#ffa500";
    private static final String GREEN = "#6fcc75";

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

    /// Creates one group per special field, each with one subgroup per value of the field.
    ///
    /// All groups are independent search groups:
    /// - Dragging an entry onto them cannot write the `groups` field, as the value of a special field is set elsewhere.
    /// - A field's group shows the entries carrying the field; it does not include its subgroups.
    ///   Including them would show the whole library, as "Not relevant" also contains the entries without the field.
    ///
    /// The top group shows the entries carrying any of the special fields.
    // [impl->req~ux.groups.suggested-marking-groups~1]
    public static GroupTreeNode createMarkingAndGradingGroups() {
        String anyMarking = MARKING_FIELDS.stream()
                                          .map(GroupsFactory::isPresent)
                                          .collect(Collectors.joining(" OR "));
        GroupTreeNode marking = GroupTreeNode.fromGroup(searchGroup(Localization.lang("Marking and grading"), anyMarking));

        GroupTreeNode rank = marking.addSubgroup(fieldGroup(Localization.lang("Rank"), SpecialField.RANKING, "RANKING"));
        rank.addSubgroup(valueGroup(Localization.lang("One star"), SpecialField.RANKING, SpecialFieldValue.RANK_1, "RANK1"));
        rank.addSubgroup(valueGroup(Localization.lang("Two stars"), SpecialField.RANKING, SpecialFieldValue.RANK_2, "RANK2"));
        rank.addSubgroup(valueGroup(Localization.lang("Three stars"), SpecialField.RANKING, SpecialFieldValue.RANK_3, "RANK3"));
        rank.addSubgroup(valueGroup(Localization.lang("Four stars"), SpecialField.RANKING, SpecialFieldValue.RANK_4, "RANK4"));
        rank.addSubgroup(valueGroup(Localization.lang("Five stars"), SpecialField.RANKING, SpecialFieldValue.RANK_5, "RANK5"));

        GroupTreeNode relevance = marking.addSubgroup(fieldGroup(Localization.lang("Relevance"), SpecialField.RELEVANCE, "RELEVANCE"));
        relevance.addSubgroup(valueGroup(Localization.lang("Relevant"), SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT, "RELEVANCE"));
        relevance.addSubgroup(withoutValueGroup(Localization.lang("Not relevant"), SpecialField.RELEVANCE, SpecialFieldValue.RELEVANT));

        GroupTreeNode quality = marking.addSubgroup(fieldGroup(Localization.lang("Quality"), SpecialField.QUALITY, "QUALITY"));
        quality.addSubgroup(valueGroup(Localization.lang("Quality assured"), SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED, "QUALITY_ASSURED"));
        quality.addSubgroup(withoutValueGroup(Localization.lang("Quality not assured"), SpecialField.QUALITY, SpecialFieldValue.QUALITY_ASSURED));

        // Not named "Printed" like its subgroup, because group names are unique within a library
        GroupTreeNode printed = marking.addSubgroup(fieldGroup(Localization.lang("Print status"), SpecialField.PRINTED, "PRINTED"));
        printed.addSubgroup(valueGroup(Localization.lang("Printed"), SpecialField.PRINTED, SpecialFieldValue.PRINTED, "PRINTED"));
        printed.addSubgroup(withoutValueGroup(Localization.lang("Not printed"), SpecialField.PRINTED, SpecialFieldValue.PRINTED));

        GroupTreeNode priority = marking.addSubgroup(fieldGroup(Localization.lang("Priority"), SpecialField.PRIORITY, "PRIORITY"));
        priority.addSubgroup(withColor(valueGroup(Localization.lang("Priority high"), SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_HIGH, "PRIORITY_HIGH"), RED));
        priority.addSubgroup(withColor(valueGroup(Localization.lang("Priority medium"), SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_MEDIUM, "PRIORITY_MEDIUM"), ORANGE));
        priority.addSubgroup(withColor(valueGroup(Localization.lang("Priority low"), SpecialField.PRIORITY, SpecialFieldValue.PRIORITY_LOW, "PRIORITY_LOW"), GREEN));

        GroupTreeNode readStatus = marking.addSubgroup(fieldGroup(Localization.lang("Read status"), SpecialField.READ_STATUS, "READ_STATUS"));
        readStatus.addSubgroup(withColor(valueGroup(Localization.lang("Read status read"), SpecialField.READ_STATUS, SpecialFieldValue.READ, "READ_STATUS_READ"), GREEN));
        readStatus.addSubgroup(withColor(valueGroup(Localization.lang("Read status skimmed"), SpecialField.READ_STATUS, SpecialFieldValue.SKIMMED, "READ_STATUS_SKIMMED"), ORANGE));

        return marking;
    }

    private static SearchGroup fieldGroup(String name, SpecialField field, String iconName) {
        SearchGroup group = searchGroup(name, isPresent(field));
        group.setIconName(iconName);
        return group;
    }

    private static SearchGroup valueGroup(String name, SpecialField field, SpecialFieldValue value, String iconName) {
        SearchGroup group = searchGroup(name, field.getName() + " == " + value.getFieldValue().orElseThrow());
        group.setIconName(iconName);
        return group;
    }

    /// The negated exact match also contains the entries without the field, which is how the entry table shows them
    private static SearchGroup withoutValueGroup(String name, SpecialField field, SpecialFieldValue value) {
        return searchGroup(name, field.getName() + " !== " + value.getFieldValue().orElseThrow());
    }

    private static SearchGroup withColor(SearchGroup group, String color) {
        group.setColor(color);
        return group;
    }

    private static String isPresent(SpecialField field) {
        return field.getName() + " =~.*";
    }

    private static SearchGroup searchGroup(String name, String searchExpression) {
        return new SearchGroup(name, GroupHierarchyType.INDEPENDENT, searchExpression, EnumSet.noneOf(SearchFlags.class));
    }
}
