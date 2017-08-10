package org.jabref.logic.groups;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

public class DefaultGroupsFactory {

    public static MaterialDesignIcon ALL_ENTRIES_GROUP_DEFAULT_ICON = MaterialDesignIcon.DATABASE;

    private DefaultGroupsFactory() {
    }

    public static AllEntriesGroup getAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconName(ALL_ENTRIES_GROUP_DEFAULT_ICON.name());
        return group;
    }
}
