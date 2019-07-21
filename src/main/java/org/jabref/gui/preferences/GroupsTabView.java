package org.jabref.gui.preferences;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class GroupsTabView extends VBox implements PrefsTab {

    @FXML private CheckBox grayNonHits;
    @FXML private RadioButton groupViewModeIntersection;
    @FXML private RadioButton groupViewModeUnion;
    @FXML private CheckBox autoAssignGroup;
    @FXML private TextField defaultGroupingField;
    @FXML private TextField keywordSeparator;

    @Inject private DialogService dialogService;
    private final JabRefPreferences preferences;

    private GroupsTabViewModel viewModel;

    public GroupsTabView(JabRefPreferences preferences) {
        this.preferences = preferences;
        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize() {
        viewModel = new GroupsTabViewModel(dialogService, preferences);

        grayNonHits.selectedProperty().bindBidirectional(viewModel.grayNonHitsProperty());
        groupViewModeIntersection.selectedProperty().bindBidirectional(viewModel.groupViewModeIntersectionProperty());
        groupViewModeUnion.selectedProperty().bindBidirectional(viewModel.groupViewModeUnionProperty());
        autoAssignGroup.selectedProperty().bindBidirectional(viewModel.autoAssignGroupProperty());
        defaultGroupingField.textProperty().bindBidirectional(viewModel.defaultGroupingFieldProperty());
        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());
    }

    @Override
    public Node getBuilder() { return this; }

    @Override
    public void setValues() {
        // Done by bindings
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public String getTabName() { return Localization.lang("Groups"); }
}
