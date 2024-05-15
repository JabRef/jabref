package org.jabref.gui.preferences.ai;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox useAi;
    @FXML private TextField openAiToken;

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferencesService, dialogService);

        useAi.selectedProperty().bindBidirectional(viewModel.useAiProperty());
        openAiToken.textProperty().bindBidirectional(viewModel.openAiTokenProperty());

        openAiToken.setDisable(!useAi.isSelected());

        useAi.selectedProperty().addListener((observable, oldValue, newValue) -> {
            openAiToken.setDisable(!newValue);
        });
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }
}
