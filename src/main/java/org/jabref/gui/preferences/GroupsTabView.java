package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class GroupsTabView extends AbstractPreferenceTabView<GroupsTabViewModel> implements PreferencesTab {

    @FXML private RadioButton groupViewModeIntersection;
    @FXML private RadioButton groupViewModeUnion;
    @FXML private CheckBox autoAssignGroup;
    @FXML private CheckBox displayGroupCount;
    @FXML private TextField keywordSeparator;

    public GroupsTabView(PreferencesService preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Groups");
    }

    public void initialize() {
        this.viewModel = new GroupsTabViewModel(dialogService, preferences);

        groupViewModeIntersection.selectedProperty().bindBidirectional(viewModel.groupViewModeIntersectionProperty());
        groupViewModeUnion.selectedProperty().bindBidirectional(viewModel.groupViewModeUnionProperty());
        autoAssignGroup.selectedProperty().bindBidirectional(viewModel.autoAssignGroupProperty());
        displayGroupCount.selectedProperty().bindBidirectional(viewModel.displayGroupCount());
        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());
    }
}
