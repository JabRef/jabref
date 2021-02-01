package org.jabref.gui.groups;

public class GroupsPreferences {

    private final GroupViewMode groupViewMode;
    private final boolean shouldAutoAssignGroup;
    private final boolean shouldDisplayGroupCount;
    private final Character keywordSeparator;

    public GroupsPreferences(GroupViewMode groupViewMode,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount,
                             Character keywordSeparator) {

        this.groupViewMode = groupViewMode;
        this.shouldAutoAssignGroup = shouldAutoAssignGroup;
        this.shouldDisplayGroupCount = shouldDisplayGroupCount;
        this.keywordSeparator = keywordSeparator;
    }

    public GroupViewMode getGroupViewMode() {
        return groupViewMode;
    }

    public boolean shouldAutoAssignGroup() {
        return shouldAutoAssignGroup;
    }

    public boolean shouldDisplayGroupCount() {
        return shouldDisplayGroupCount;
    }

    public Character getKeywordDelimiter() {
        return keywordSeparator;
    }
}
