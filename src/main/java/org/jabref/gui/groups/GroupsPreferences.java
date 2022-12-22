package org.jabref.gui.groups;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GroupsPreferences {

    private final ObjectProperty<GroupViewMode> groupViewMode;
    private final BooleanProperty shouldAutoAssignGroup;
    private final BooleanProperty shouldDisplayGroupCount;

    public GroupsPreferences(GroupViewMode groupViewMode,
                             boolean shouldAutoAssignGroup,
                             boolean shouldDisplayGroupCount) {

        this.groupViewMode = new SimpleObjectProperty<>(groupViewMode);
        this.shouldAutoAssignGroup = new SimpleBooleanProperty(shouldAutoAssignGroup);
        this.shouldDisplayGroupCount = new SimpleBooleanProperty(shouldDisplayGroupCount);
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
}
