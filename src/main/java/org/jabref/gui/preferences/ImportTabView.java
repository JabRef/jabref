package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportTabView extends AbstractPreferenceTabView<ImportTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<String> fileNamePattern;
    @FXML private TextField fileDirPattern;

    public ImportTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        this.viewModel = new ImportTabViewModel(dialogService, preferences);

        fileNamePattern.valueProperty().bindBidirectional(viewModel.fileNamePatternProperty());
        fileNamePattern.itemsProperty().bind(viewModel.defaultFileNamePatternsProperty());
        fileDirPattern.textProperty().bindBidirectional(viewModel.fileDirPatternProperty());
    }

    @Override
    public String getTabName() { return Localization.lang("Import"); }
}
