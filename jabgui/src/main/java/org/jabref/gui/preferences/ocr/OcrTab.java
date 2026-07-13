package org.jabref.gui.preferences.ocr;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.PagesWithTextHandling;

import com.airhacks.afterburner.views.ViewLoader;

public class OcrTab extends AbstractPreferenceTabView<OcrTabViewModel> implements PreferencesTab {
    @FXML private TextField ocrEnginePath;
    @FXML private Button browseButton;
    @FXML private Button autoDetectButton;
    @FXML private ComboBox<PagesWithTextHandling> pagesHaveTextComboBox;

    public OcrTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("OCR");
    }

    public void initialize() {
        this.viewModel = new OcrTabViewModel(dialogService, preferences.getFilePreferences(), preferences.getOcrPreferences(), taskExecutor);

        initializeOcrEnginePath();
        initializePagesHaveText();
    }

    private void initializeOcrEnginePath() {
        ocrEnginePath.textProperty().bindBidirectional(viewModel.ocrEnginePathProperty());
    }

    private void initializePagesHaveText() {
        new ViewModelListCellFactory<PagesWithTextHandling>()
                .withText(PagesWithTextHandling::getDisplayName)
                .install(pagesHaveTextComboBox);
        pagesHaveTextComboBox.itemsProperty().bind(viewModel.pagesHaveTextOptions());
        pagesHaveTextComboBox.valueProperty().bindBidirectional(viewModel.selectedPagesHaveTextProperty());
    }

    @FXML
    private void browseEnginePath() {
        viewModel.browseEnginePath();
    }

    @FXML
    private void autoDetectEnginePath() {
        viewModel.autoDetectEnginePath();
    }
}
