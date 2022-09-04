package org.jabref.gui.groups;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public class GroupsPreferences {

    private final SetProperty<GroupViewMode> groupViewMode;
    private final BooleanProperty shouldAutoAssignGroup;
    private final BooleanProperty shouldDisplayGroupCount;
    private final ObjectProperty<Character> keywordSeparator;

    public GroupsPreferences(boolean viewModeIntersection,
                             boolean viewModeFilter,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount,
                             ObjectProperty<Character> keywordSeparator) {

        this.groupViewMode = new SimpleSetProperty<>(FXCollections.observableSet());
        if (viewModeIntersection) {
            this.groupViewMode.add(GroupViewMode.INTERSECTION);
        }
        if (viewModeFilter) {
            this.groupViewMode.add(GroupViewMode.FILTER);
        }
        this.shouldAutoAssignGroup = new SimpleBooleanProperty(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount = new SimpleBooleanProperty(shouldDisplayGroupCount);
        this.keywordSeparator = keywordSeparator;
    }

    public EnumSet<GroupViewMode> getGroupViewMode() {
        if (groupViewMode.isEmpty()) {
            return EnumSet.noneOf(GroupViewMode.class);
        }
        return EnumSet.copyOf(groupViewMode);
    }

    public SetProperty<GroupViewMode> groupViewModeProperty() {
        return groupViewMode;
    }

    public void setGroupViewMode(GroupViewMode mode, boolean value) {
        if (groupViewMode.contains(mode) && !value) {
            groupViewMode.remove(mode);
        } else if (!groupViewMode.contains(mode) && value) {
            groupViewMode.add(mode);
        }
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
