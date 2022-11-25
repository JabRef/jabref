package org.jabref.gui.groups;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GroupsPreferences {

    private final ObjectProperty<GroupViewMode> groupViewMode;
    private final BooleanProperty shouldAutoAssignGroup;
    private final BooleanProperty shouldDisplayGroupCount;
    private final ObjectProperty<Character> keywordSeparator;

    public GroupsPreferences(GroupViewMode groupViewMode,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount,
                             ObjectProperty<Character> keywordSeparator) {

        this.groupViewMode = new SimpleObjectProperty<>(groupViewMode);
        this.shouldAutoAssignGroup = new SimpleBooleanProperty(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount = new SimpleBooleanProperty(shouldDisplayGroupCount);
        this.keywordSeparator = keywordSeparator;
    }

    public GroupViewMode getGroupViewMode() {
        return groupViewMode.getValue();
    }

    public ObjectProperty<GroupViewMode> groupViewModeProperty() {
        return groupViewMode;
    }

    public void setGroupViewMode(GroupViewMode groupViewMode) {
        this.groupViewMode.set(groupViewMode);
    }

    public boolean shouldAutoAssignGroup() {
        return shouldAutoAssignGroup.getValue();
    }

    public BooleanProperty autoAssignGroupProperty() {
        return shouldAutoAssignGroup;
    }

    public void setAutoAssignGroup(boolean shouldAutoAssignGroup) {
        this.shouldAutoAssignGroup.set(shouldAutoAssignGroup);
    }

    public boolean shouldDisplayGroupCount() {
        return shouldDisplayGroupCount.getValue();
    }

    public BooleanProperty displayGroupCountProperty() {
        return shouldDisplayGroupCount;
    }

    public void setDisplayGroupCount(boolean shouldDisplayGroupCount) {
        this.shouldDisplayGroupCount.set(shouldDisplayGroupCount);
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.getValue();
    }

    public ObjectProperty<Character> keywordSeparatorProperty() {
        return keywordSeparator;
    }

    public void setKeywordSeparator(Character keywordSeparator) {
        this.keywordSeparator.set(keywordSeparator);
    }
}
