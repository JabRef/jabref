package org.jabref.logic.groups;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

public class DefaultGroupsFactory {

    private DefaultGroupsFactory() {
    }

    public static AllEntriesGroup getAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconName(IconTheme.JabRefIcons.ALL_ENTRIES_GROUP_ICON.name());
        return group;
    }
}
