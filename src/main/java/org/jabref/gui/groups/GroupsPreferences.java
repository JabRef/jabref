package org.jabref.gui.groups;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public class GroupsPreferences {

    private final SetProperty<GroupViewMode> groupViewMode = new SimpleSetProperty<>(FXCollections.observableSet());;
    private final BooleanProperty shouldAutoAssignGroup = new SimpleBooleanProperty();
    private final BooleanProperty shouldDisplayGroupCount = new SimpleBooleanProperty();

    public GroupsPreferences(boolean viewModeIntersection,
                             boolean viewModeFilter,
                             boolean viewModeInvert,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount) {
        if (viewModeIntersection) {
            this.groupViewMode.add(GroupViewMode.INTERSECTION);
        }
        if (viewModeFilter) {
            this.groupViewMode.add(GroupViewMode.FILTER);
        }
        if (viewModeInvert) {
            this.groupViewMode.add(GroupViewMode.INVERT);
        }
        this.shouldAutoAssignGroup.set(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount.set(shouldDisplayGroupCount);
    }

    public GroupsPreferences(EnumSet<GroupViewMode> groupViewModes, boolean shouldAutoAssignGroup, boolean shouldDisplayGroupCount) {
        this.groupViewMode.addAll(groupViewModes);
        this.shouldAutoAssignGroup.set(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount.set(shouldDisplayGroupCount);
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
}
