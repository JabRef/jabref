package org.jabref.gui.preferences.groups;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GroupsTab extends AbstractPreferenceTabView<GroupsTabViewModel> implements PreferencesTab {

    @FXML private RadioButton groupViewModeIntersection;
    @FXML private RadioButton groupViewModeUnion;
    @FXML private CheckBox autoAssignGroup;
    @FXML private CheckBox displayGroupCount;
    @FXML private TextField keywordSeparator;

    public GroupsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Groups");
    }

    public void initialize() {
        this.viewModel = new GroupsTabViewModel(dialogService, preferencesService);

        groupViewModeIntersection.selectedProperty().bindBidirectional(viewModel.groupViewModeIntersectionProperty());
        groupViewModeUnion.selectedProperty().bindBidirectional(viewModel.groupViewModeUnionProperty());
        autoAssignGroup.selectedProperty().bindBidirectional(viewModel.autoAssignGroupProperty());
        displayGroupCount.selectedProperty().bindBidirectional(viewModel.displayGroupCount());
        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());
    }
}
