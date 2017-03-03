package org.jabref.logic.groups;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

public class DefaultGroupsFactory {
    public static AllEntriesGroup getAllEntriesGroup() {
        return new AllEntriesGroup(Localization.lang("All entries"));
    }
}
