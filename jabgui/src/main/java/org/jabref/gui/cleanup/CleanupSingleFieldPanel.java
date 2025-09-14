package org.jabref.gui.cleanup;

import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanups;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupSingleFieldPanel extends VBox {

    @FXML private FieldFormatterCleanupsPanel formatterCleanupsPanel;

    private final CleanupDialogViewModel viewModel;

    public CleanupSingleFieldPanel(CleanupPreferences cleanupPreferences,
                                   CleanupDialogViewModel viewModel) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");
        Objects.requireNonNull(viewModel, "viewModel must not be null");

        this.viewModel = viewModel;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(cleanupPreferences);
    }

    @FXML
    private void onApply() {
        CleanupTabSelection selectedTab = CleanupTabSelection.ofFormatters(getSelectedFormatters());
        viewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }

    private void init(CleanupPreferences cleanupPreferences) {
        formatterCleanupsPanel.cleanupsDisableProperty().setValue(!cleanupPreferences.getFieldFormatterCleanups().isEnabled());
        formatterCleanupsPanel.cleanupsProperty().setValue(FXCollections.observableArrayList(
                cleanupPreferences.getFieldFormatterCleanups().getConfiguredActions()
        ));
    }

    public FieldFormatterCleanups getSelectedFormatters() {
        return new FieldFormatterCleanups(
                !formatterCleanupsPanel.cleanupsDisableProperty().getValue(),
                formatterCleanupsPanel.cleanupsProperty()
        );
    }
}
