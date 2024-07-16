package org.jabref.gui.preferences.groups;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final BooleanProperty displayGroupCountProperty = new SimpleBooleanProperty();

    private final GroupsPreferences groupsPreferences;

    public GroupsTabViewModel(GroupsPreferences groupsPreferences) {
        this.groupsPreferences = groupsPreferences;
    }

    @Override
    public void setValues() {
        if (groupsPreferences.getGroupViewMode().contains(GroupViewMode.INTERSECTION)) {
            groupViewModeIntersectionProperty.setValue(true);
            groupViewModeUnionProperty.setValue(false);
        } else {
            groupViewModeIntersectionProperty.setValue(false);
            groupViewModeUnionProperty.setValue(true);
        }
        autoAssignGroupProperty.setValue(groupsPreferences.shouldAutoAssignGroup());
        displayGroupCountProperty.setValue(groupsPreferences.shouldDisplayGroupCount());
    }

    @Override
    public void storeSettings() {
        groupsPreferences.setGroupViewMode(GroupViewMode.INTERSECTION, groupViewModeIntersectionProperty.getValue());
        groupsPreferences.setAutoAssignGroup(autoAssignGroupProperty.getValue());
        groupsPreferences.setDisplayGroupCount(displayGroupCountProperty.getValue());
    }

    public BooleanProperty groupViewModeIntersectionProperty() {
        return groupViewModeIntersectionProperty;
    }

    public BooleanProperty groupViewModeUnionProperty() {
        return groupViewModeUnionProperty;
    }

    public BooleanProperty autoAssignGroupProperty() {
        return autoAssignGroupProperty;
    }

    public BooleanProperty displayGroupCount() {
        return displayGroupCountProperty;
    }
}
