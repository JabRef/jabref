package org.jabref.gui.preferences.ocr;

import java.nio.file.Path;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;

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
        this.viewModel = new OcrTabViewModel(preferences.getOcrPreferences());

        ocrPath.textProperty().bindBidirectional(viewModel.ocrPathProperty());
    }

    @FXML
    private void browseEnginePath() {
        Optional<Path> selectedPath = dialogService.showFileOpenDialog(
                new FileDialogConfiguration.Builder()
                        .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                        .build());
        selectedPath.ifPresent(path -> viewModel.ocrPathProperty().set(path.toString()));
    }

    @FXML
    private void autoDetectEnginePath() {
        BackgroundTask<Optional<String>> autoDetectTask = BackgroundTask.wrap(() -> viewModel.autoDetectEnginePath());

        autoDetectTask.titleProperty().set(Localization.lang("Auto detection of engine path"));
        autoDetectTask.showToUser(true);
        autoDetectTask.onSuccess(result -> {
            if (result.isPresent()) {
                viewModel.ocrPathProperty().set(result.get());
                dialogService.notify(Localization.lang("OCRmyPDF detected at: %0", result.get()));
            } else {
                dialogService.notify(Localization.lang("OCRmyPDF could not be detected automatically"));
            }
        });
        autoDetectTask.onFailure(_ -> dialogService.notify(Localization.lang("Auto detect engine path failed")));
        taskExecutor.execute(autoDetectTask);
    }
}
