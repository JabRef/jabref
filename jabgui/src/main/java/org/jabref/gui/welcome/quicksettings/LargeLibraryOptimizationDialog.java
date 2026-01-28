package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;

import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.quicksettings.viewmodel.LargeLibraryOptimizationDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class LargeLibraryOptimizationDialog extends FXDialog {
    @FXML private CheckBox disableFulltextIndexing;
    @FXML private CheckBox disableCreationDate;
    @FXML private CheckBox disableModificationDate;
    @FXML private CheckBox disableAutosave;
    @FXML private CheckBox disableGroupCount;
    @FXML private HelpButton helpButton;

    private LargeLibraryOptimizationDialogViewModel viewModel;
    private final GuiPreferences preferences;

    public LargeLibraryOptimizationDialog(GuiPreferences preferences) {
        super(AlertType.NONE, Localization.lang("Optimize for large libraries"), true);

        this.preferences = preferences;

        this.setHeaderText(Localization.lang("Improve performance when working with libraries containing many entries"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.saveSettings();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new LargeLibraryOptimizationDialogViewModel(preferences);

        disableFulltextIndexing.selectedProperty().bindBidirectional(viewModel.disableFulltextIndexingProperty());
        disableCreationDate.selectedProperty().bindBidirectional(viewModel.disableCreationDateProperty());
        disableModificationDate.selectedProperty().bindBidirectional(viewModel.disableModificationDateProperty());
        disableAutosave.selectedProperty().bindBidirectional(viewModel.disableAutosaveProperty());
        disableGroupCount.selectedProperty().bindBidirectional(viewModel.disableGroupCountProperty());

        helpButton.setHelpUrl(URLs.PERFORMANCE_DOC);
    }
}
