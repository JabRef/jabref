package org.jabref.gui.preferences.groups;

import javafx.beans.property.*;

import javafx.collections.FXCollections;
import org.jabref.gui.DialogService;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.model.groups.GroupHierarchyType;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final BooleanProperty displayGroupCountProperty = new SimpleBooleanProperty();
    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");
    private final ListProperty<GroupHierarchyType> groupDefaultListProperty = new SimpleListProperty<>();

    private final ObjectProperty<GroupHierarchyType> selectedGroupDefaultProperty = new SimpleObjectProperty<>();

    private final DialogService dialogService;
    private final GroupsPreferences groupsPreferences;

    public GroupsTabViewModel(DialogService dialogService, GroupsPreferences groupsPreferences) {
        this.dialogService = dialogService;
        this.groupsPreferences = groupsPreferences;
    }

    @Override
    public void setValues() {
        switch (groupsPreferences.getGroupViewMode()) {
            case INTERSECTION -> {
                groupViewModeIntersectionProperty.setValue(true);
                groupViewModeUnionProperty.setValue(false);
            }
            case UNION -> {
                groupViewModeIntersectionProperty.setValue(false);
                groupViewModeUnionProperty.setValue(true);
            }
        }
        groupDefaultListProperty.setValue(FXCollections.observableArrayList(GroupHierarchyType.values()));
        selectedGroupDefaultProperty.setValue(groupsPreferences.getDefaultGroupMode());


        autoAssignGroupProperty.setValue(groupsPreferences.shouldAutoAssignGroup());
        displayGroupCountProperty.setValue(groupsPreferences.shouldDisplayGroupCount());
        keywordSeparatorProperty.setValue(groupsPreferences.getKeywordSeparator().toString());
    }

    @Override
    public void storeSettings() {
        groupsPreferences.setGroupViewMode(groupViewModeIntersectionProperty.getValue() ? GroupViewMode.INTERSECTION : GroupViewMode.UNION);
        groupsPreferences.setAutoAssignGroup(autoAssignGroupProperty.getValue());
        groupsPreferences.setDisplayGroupCount(displayGroupCountProperty.getValue());
        groupsPreferences.keywordSeparatorProperty().setValue(keywordSeparatorProperty.getValue().charAt(0));
        groupsPreferences.setDefaultGroupCreate(selectedGroupDefaultProperty.getValue());
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

    public ListProperty<GroupHierarchyType> defaultGroupsListProperty() {
        return this.groupDefaultListProperty;
    }

    public Property<GroupHierarchyType> selectedDefaultGroup() {
        return this.selectedGroupDefaultProperty;
    }
}
