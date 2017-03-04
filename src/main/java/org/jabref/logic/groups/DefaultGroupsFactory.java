package org.jabref.logic.groups;

import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

public class DefaultGroupsFactory {
    public static AllEntriesGroup getAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup(Localization.lang("All entries"));
        group.setIconCode(IconTheme.JabRefIcon.ALL_ENTRIES_GROUP_ICON.getCode());
        return group;
    }
}
