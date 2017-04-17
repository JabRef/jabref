package org.jabref.logic.groups;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

public class DefaultGroupsFactory {

    public static String ALL_ENTRIES_GROUP_DEFAULT_ICON = "\uF1B8"; /* css: database */

    private DefaultGroupsFactory() {
    }

    public static AllEntriesGroup getAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconCode(ALL_ENTRIES_GROUP_DEFAULT_ICON);
        return group;
    }
}
