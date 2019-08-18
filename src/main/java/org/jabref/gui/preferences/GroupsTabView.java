package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class GroupsTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private CheckBox grayNonHits;
    @FXML private RadioButton groupViewModeIntersection;
    @FXML private RadioButton groupViewModeUnion;
    @FXML private CheckBox autoAssignGroup;
    @FXML private TextField defaultGroupingField;
    @FXML private TextField keywordSeparator;

    public GroupsTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Groups"); }

    public void initialize() {
        GroupsTabViewModel groupsTabViewModel = new GroupsTabViewModel(dialogService, preferences);
        this.viewModel = groupsTabViewModel;

        grayNonHits.selectedProperty().bindBidirectional(groupsTabViewModel.grayNonHitsProperty());
        groupViewModeIntersection.selectedProperty().bindBidirectional(groupsTabViewModel.groupViewModeIntersectionProperty());
        groupViewModeUnion.selectedProperty().bindBidirectional(groupsTabViewModel.groupViewModeUnionProperty());
        autoAssignGroup.selectedProperty().bindBidirectional(groupsTabViewModel.autoAssignGroupProperty());
        defaultGroupingField.textProperty().bindBidirectional(groupsTabViewModel.defaultGroupingFieldProperty());
        keywordSeparator.textProperty().bindBidirectional(groupsTabViewModel.keywordSeparatorProperty());
    }
}
