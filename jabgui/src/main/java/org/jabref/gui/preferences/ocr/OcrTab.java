package org.jabref.gui.preferences.ocr;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class OcrTab extends AbstractPreferenceTabView<OcrTabViewModel> implements PreferencesTab {
    @FXML private TextField ocrPath;
    @FXML private Button browseButton;
    @FXML private Button autoDetectButton;

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
        this.viewModel = new OcrTabViewModel(dialogService, preferences, taskExecutor);

        ocrPath.textProperty().bindBidirectional(viewModel.ocrPathProperty());
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
