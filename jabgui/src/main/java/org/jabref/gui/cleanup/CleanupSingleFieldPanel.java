package org.jabref.gui.cleanup;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.cleanup.FieldFormatterCleanups;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupSingleFieldPanel extends VBox {

    @FXML private FieldFormatterCleanupsPanel formatterCleanupsPanel;

    private final CleanupSingleFieldViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupSingleFieldPanel(CleanupPreferences cleanupPreferences,
                                   CleanupDialogViewModel dialogViewModel) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");
        Objects.requireNonNull(dialogViewModel, "viewModel must not be null");

        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupSingleFieldViewModel(cleanupPreferences.getFieldFormatterCleanups());

        ViewLoader.view(this)
                  .root(this)
                  .load();

        bindProperties();
    }

    private void bindProperties() {
        formatterCleanupsPanel.cleanupsDisableProperty().bind(viewModel.cleanupsEnabled.not());
        formatterCleanupsPanel.cleanupsProperty().bindBidirectional(viewModel.cleanups);
    }

    @FXML
    private void onApply() {
        FieldFormatterCleanups selectedFormatters = viewModel.getSelectedFormatters();
        CleanupTabSelection selectedTab = CleanupTabSelection.ofFormatters(selectedFormatters);
        dialogViewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }
}
