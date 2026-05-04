package org.jabref.logic.groups;

import java.util.EnumSet;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

public class GroupsFactory {

    public static final String ALL_ENTRIES_GROUP_DEFAULT_ICON = "ALL_ENTRIES_GROUP_ICON";

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
}
