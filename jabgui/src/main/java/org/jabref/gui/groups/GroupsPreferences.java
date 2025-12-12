package org.jabref.gui.groups;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.model.groups.GroupHierarchyType;

import com.google.common.annotations.VisibleForTesting;

public class GroupsPreferences {

    private final SetProperty<GroupViewMode> groupViewMode;
    private final BooleanProperty shouldAutoAssignGroup;
    private final BooleanProperty shouldDisplayGroupCount;
    private final ObjectProperty<GroupHierarchyType> defaultHierarchicalContext;

    public GroupsPreferences(boolean viewModeIntersection,
                             boolean viewModeFilter,
                             boolean viewModeInvert,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount,
                             GroupHierarchyType defaultHierarchicalContext) {

        this.groupViewMode = new SimpleSetProperty<>(FXCollections.observableSet());
        this.shouldAutoAssignGroup = new SimpleBooleanProperty(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount = new SimpleBooleanProperty(shouldDisplayGroupCount);
        this.defaultHierarchicalContext = new SimpleObjectProperty<>(defaultHierarchicalContext);

        if (viewModeIntersection) {
            this.groupViewMode.add(GroupViewMode.INTERSECTION);
        }
        if (viewModeFilter) {
            this.groupViewMode.add(GroupViewMode.FILTER);
        }
        if (viewModeInvert) {
            this.groupViewMode.add(GroupViewMode.INVERT);
        }
    }

    public GroupsPreferences() {
        this(
                true,           // Default view mode intersection
                true,                             // Default view mode filter
                false,                            // Default view mode invert
                true,                             // Default auto assign group
                true,                             // Default display group content
                GroupHierarchyType.INDEPENDENT    // Default hierarchical context
        );
    }

    @VisibleForTesting
    public GroupsPreferences(EnumSet<GroupViewMode> groupViewMode,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount,
                             GroupHierarchyType defaultHierarchicalContext) {
        this.groupViewMode = new SimpleSetProperty<>(FXCollections.observableSet(groupViewMode));
        this.shouldAutoAssignGroup = new SimpleBooleanProperty(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount = new SimpleBooleanProperty(shouldDisplayGroupCount);
        this.defaultHierarchicalContext = new SimpleObjectProperty<>(defaultHierarchicalContext);
    }

    public static GroupsPreferences getDefault() {
        return new GroupsPreferences();
    }

    public void setAll(GroupsPreferences preferences) {
        this.groupViewMode.set(preferences.groupViewMode);
        this.shouldAutoAssignGroup.set(preferences.shouldAutoAssignGroup());
        this.shouldDisplayGroupCount.set(preferences.shouldDisplayGroupCount());
        this.defaultHierarchicalContext.set(preferences.getDefaultHierarchicalContext());
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
        if (value) {
            groupViewMode.add(mode);
        } else {
            groupViewMode.remove(mode);
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

    public GroupHierarchyType getDefaultHierarchicalContext() {
        return defaultHierarchicalContext.get();
    }

    public ObjectProperty<GroupHierarchyType> defaultHierarchicalContextProperty() {
        return defaultHierarchicalContext;
    }

    public void setDefaultHierarchicalContext(GroupHierarchyType defaultHierarchicalContext) {
        this.defaultHierarchicalContext.set(defaultHierarchicalContext);
    }
}
