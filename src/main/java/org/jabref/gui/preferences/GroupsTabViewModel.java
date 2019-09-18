package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.preferences.JabRefPreferences;

public class GroupsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty grayNonHitsProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty groupViewModeUnionProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoAssignGroupProperty = new SimpleBooleanProperty();
    private final StringProperty defaultGroupingFieldProperty = new SimpleStringProperty("");
    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public GroupsTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        grayNonHitsProperty.setValue(preferences.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS));
        switch (preferences.getGroupViewMode()) {
            case INTERSECTION:
                groupViewModeIntersectionProperty.setValue(true);
                groupViewModeUnionProperty.setValue(false);
                break;
            case UNION:
                groupViewModeIntersectionProperty.setValue(false);
                groupViewModeUnionProperty.setValue(true);
                break;
        }
        autoAssignGroupProperty.setValue(preferences.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP));

        defaultGroupingFieldProperty.setValue(preferences.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        keywordSeparatorProperty.setValue(preferences.get(JabRefPreferences.KEYWORD_SEPARATOR));
    }

    @Override
    public void storeSettings() {
        preferences.putBoolean(JabRefPreferences.GRAY_OUT_NON_HITS, grayNonHitsProperty.getValue());
        if (groupViewModeIntersectionProperty.getValue()) {
            preferences.setGroupViewMode(GroupViewMode.INTERSECTION);
        } else if (groupViewModeUnionProperty.getValue()) {
            preferences.setGroupViewMode(GroupViewMode.UNION);
        }
        preferences.putBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP, autoAssignGroupProperty.getValue());

        preferences.put(JabRefPreferences.GROUPS_DEFAULT_FIELD, defaultGroupingFieldProperty.getValue().trim());
        preferences.put(JabRefPreferences.KEYWORD_SEPARATOR, keywordSeparatorProperty.getValue());
    }

    @Override
    public boolean validateSettings() { return true; }

    @Override
    public List<String> getRestartWarnings() { return new ArrayList<>(); }

    public BooleanProperty grayNonHitsProperty() { return grayNonHitsProperty; }

    public BooleanProperty groupViewModeIntersectionProperty() { return groupViewModeIntersectionProperty; }

    public BooleanProperty groupViewModeUnionProperty() { return groupViewModeUnionProperty; }

    public BooleanProperty autoAssignGroupProperty() { return autoAssignGroupProperty; }

    public StringProperty defaultGroupingFieldProperty() { return defaultGroupingFieldProperty; }

    public StringProperty keywordSeparatorProperty() { return keywordSeparatorProperty; }
}
