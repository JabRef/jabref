package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;

import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.quicksettings.viewmodel.EntryTableConfigurationDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class EntryTableConfigurationDialog extends FXDialog {
    @FXML private CheckBox showCitationKeyBox;
    @FXML private HelpButton helpButton;

    private EntryTableConfigurationDialogViewModel viewModel;
    private final GuiPreferences preferences;

    public EntryTableConfigurationDialog(GuiPreferences preferences) {
        super(AlertType.NONE, Localization.lang("Customize entry table"), true);

        this.preferences = preferences;

        this.setHeaderText(Localization.lang("Configure which columns are displayed in the entry table"));

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
        viewModel = new EntryTableConfigurationDialogViewModel(preferences);

        showCitationKeyBox.selectedProperty().bindBidirectional(viewModel.showCitationKeyProperty());

        helpButton.setHelpPage(URLs.ENTRY_TABLE_COLUMNS_DOC);
    }
}
