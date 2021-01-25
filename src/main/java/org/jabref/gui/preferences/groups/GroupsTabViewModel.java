package org.jabref.gui.preferences.groups;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.PreferencesService;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final BooleanProperty displayGroupCountProperty = new SimpleBooleanProperty();
    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final GroupsPreferences initialGroupsPreferences;

    public GroupsTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialGroupsPreferences = preferences.getGroupsPreferences();
    }

    @Override
    public void setValues() {
        switch (initialGroupsPreferences.getGroupViewMode()) {
            case INTERSECTION -> {
                groupViewModeIntersectionProperty.setValue(true);
                groupViewModeUnionProperty.setValue(false);
            }
            case UNION -> {
                groupViewModeIntersectionProperty.setValue(false);
                groupViewModeUnionProperty.setValue(true);
            }
        }
        autoAssignGroupProperty.setValue(initialGroupsPreferences.shouldAutoAssignGroup());
        displayGroupCountProperty.setValue(initialGroupsPreferences.shouldDisplayGroupCount());
        keywordSeparatorProperty.setValue(initialGroupsPreferences.getKeywordDelimiter().toString());
    }

    @Override
    public void storeSettings() {
        GroupViewMode groupViewMode = GroupViewMode.UNION;
        if (groupViewModeIntersectionProperty.getValue()) {
            groupViewMode = GroupViewMode.INTERSECTION;
        }

        GroupsPreferences newGroupsPreferences = new GroupsPreferences(
                groupViewMode,
                autoAssignGroupProperty.getValue(),
                displayGroupCountProperty.getValue(),
                keywordSeparatorProperty.getValue().charAt(0));
        preferences.storeGroupsPreferences(newGroupsPreferences);
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
