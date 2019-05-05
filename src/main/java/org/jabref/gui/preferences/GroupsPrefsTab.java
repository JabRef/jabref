package org.jabref.gui.preferences;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

class GroupsPrefsTab extends Pane implements PrefsTab {

    private final CheckBox hideNonHits = new CheckBox(Localization.lang("Hide non-hits"));
    private final CheckBox grayOut = new CheckBox(Localization.lang("Gray out non-hits"));
    private final CheckBox autoAssignGroup = new CheckBox(Localization.lang("Automatically assign new entry to selected groups"));
    private final RadioButton multiSelectionModeIntersection = new RadioButton(Localization.lang("Intersection"));
    private final RadioButton multiSelectionModeUnion = new RadioButton(Localization.lang("Union"));

    private final TextField groupingField = new TextField();
    private final TextField keywordSeparator = new TextField();
    private final GridPane builder = new GridPane();
    private final JabRefPreferences prefs;

    public GroupsPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        builder.setVgap(7);

        keywordSeparator.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                keywordSeparator.selectAll();
            }
        });

        multiSelectionModeIntersection.setText(Localization.lang("Display only entries belonging to all selected groups."));
        multiSelectionModeUnion.setText(Localization.lang("Display all entries belonging to one or more of the selected groups."));

        // View title
        Label view = new Label(Localization.lang("View"));
        view.getStyleClass().add("sectionHeader");
        builder.add(view, 1, 1);

        // View configuration
        builder.add(hideNonHits, 1, 3);
        builder.add(grayOut, 1, 4);
        final ToggleGroup selectionModeGroup = new ToggleGroup();
        builder.add(multiSelectionModeIntersection, 1, 5);
        builder.add(multiSelectionModeUnion, 1, 6);
        multiSelectionModeIntersection.setToggleGroup(selectionModeGroup);
        multiSelectionModeUnion.setToggleGroup(selectionModeGroup);
        builder.add(autoAssignGroup, 1, 7);

        builder.add(new Separator(), 1, 11);

        // Dynamic groups title
        Label dynamicGroups = new Label(Localization.lang("Dynamic groups"));
        dynamicGroups.getStyleClass().add("sectionHeader");
        builder.add(dynamicGroups, 1, 13);

        // Dynamic groups configuration
        Label defaultGrouping = new Label(Localization.lang("Default grouping field") + ":");
        builder.add(defaultGrouping, 1, 15);
        builder.add(groupingField, 2, 15);
        Label label = new Label(Localization.lang("When adding/removing keywords, separate them by") + ":");
        builder.add(label, 1, 17);
        builder.add(keywordSeparator, 2, 17);
    }

    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        grayOut.setSelected(prefs.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS));
        groupingField.setText(prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        keywordSeparator.setText(prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
        autoAssignGroup.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP));

        GroupViewMode mode = prefs.getGroupViewMode();
        if (mode == GroupViewMode.INTERSECTION) {
            multiSelectionModeIntersection.setSelected(true);
        }
        if (mode == GroupViewMode.UNION) {
            multiSelectionModeUnion.setSelected(true);
        }
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.GRAY_OUT_NON_HITS, grayOut.isSelected());
        prefs.put(JabRefPreferences.GROUPS_DEFAULT_FIELD, groupingField.getText().trim());
        prefs.putBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP, autoAssignGroup.isSelected());
        prefs.put(JabRefPreferences.KEYWORD_SEPARATOR, keywordSeparator.getText());

        if (multiSelectionModeIntersection.isSelected()) {
            prefs.setGroupViewMode(GroupViewMode.INTERSECTION);
        }
        if (multiSelectionModeUnion.isSelected()) {
            prefs.setGroupViewMode(GroupViewMode.UNION);
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Groups");
    }

}
