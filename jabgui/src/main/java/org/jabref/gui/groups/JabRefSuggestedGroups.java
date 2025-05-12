package org.jabref.gui.groups;

import java.util.EnumSet;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

public class JabRefSuggestedGroups {

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
