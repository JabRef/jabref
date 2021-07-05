package org.jabref.logic.groups;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

public class DefaultGroupsFactory {

    private static String ALL_ENTRIES_GROUP_DEFAULT_ICON = "ALL_ENTRIES_GROUP_ICON";

    private DefaultGroupsFactory() {
    }

    public static AllEntriesGroup getAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconName(ALL_ENTRIES_GROUP_DEFAULT_ICON);
        return group;
    }
}
