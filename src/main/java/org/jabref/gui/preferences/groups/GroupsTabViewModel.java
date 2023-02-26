package org.jabref.gui.preferences.groups;

import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.model.groups.GroupHierarchyType;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final BooleanProperty displayGroupCountProperty = new SimpleBooleanProperty();
    private final ListProperty<GroupHierarchyType> hierarchicalContextListProperty = new SimpleListProperty<>();
    private final ObjectProperty<GroupHierarchyType> selectedHierarchicalContextProperty = new SimpleObjectProperty<>();

    private final GroupsPreferences groupsPreferences;

    public GroupsTabViewModel(GroupsPreferences groupsPreferences) {
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
        autoAssignGroupProperty.setValue(groupsPreferences.shouldAutoAssignGroup());
        displayGroupCountProperty.setValue(groupsPreferences.shouldDisplayGroupCount());
        hierarchicalContextListProperty.setValue(
                new SortedList<>(FXCollections.observableArrayList(GroupHierarchyType.values()), Comparator.comparing(GroupHierarchyType::getDisplayName)));
        selectedHierarchicalContextProperty.setValue(groupsPreferences.getDefaultHierarchicalContext());
    }

    @Override
    public void storeSettings() {
        groupsPreferences.setGroupViewMode(groupViewModeIntersectionProperty.getValue() ? GroupViewMode.INTERSECTION : GroupViewMode.UNION);
        groupsPreferences.setAutoAssignGroup(autoAssignGroupProperty.getValue());
        groupsPreferences.setDisplayGroupCount(displayGroupCountProperty.getValue());
        groupsPreferences.setDefaultHierarchicalContext(selectedHierarchicalContextProperty.getValue());
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

    public ListProperty<GroupHierarchyType> hierarchicalContextListProperty() {
        return hierarchicalContextListProperty;
    }

    public ObjectProperty<GroupHierarchyType> selectedHierarchicalContextProperty() {
        return selectedHierarchicalContextProperty;
    }
}
