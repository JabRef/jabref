package org.jabref.gui.preferences.groups;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final BooleanProperty displayGroupCountProperty = new SimpleBooleanProperty();
    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final GroupsPreferences groupsPreferences;

    public GroupsTabViewModel(DialogService dialogService, GroupsPreferences groupsPreferences) {
        this.dialogService = dialogService;
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
        keywordSeparatorProperty.setValue(groupsPreferences.getKeywordSeparator().toString());
    }

    @Override
    public void storeSettings() {
        groupsPreferences.setGroupViewMode(GroupViewMode.INTERSECTION, groupViewModeIntersectionProperty.getValue());
        groupsPreferences.setAutoAssignGroup(autoAssignGroupProperty.getValue());
        groupsPreferences.setDisplayGroupCount(displayGroupCountProperty.getValue());
        groupsPreferences.keywordSeparatorProperty().setValue(keywordSeparatorProperty.getValue().charAt(0));
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

    public StringProperty keywordSeparatorProperty() {
        return keywordSeparatorProperty;
    }
}
