package org.jabref.gui.preferences;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeGeneralFieldsTabView extends AbstractPreferenceTabView<CustomizeGeneralFieldsTabViewModel> implements PreferencesTab {

    @FXML private Button generalFieldsHelp;
    @FXML private TextArea fieldsTextArea;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;

    public CustomizeGeneralFieldsTabView() {

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new CustomizeGeneralFieldsTabViewModel(dialogService, preferences);

        fieldsTextArea.textProperty().bindBidirectional(viewModel.fieldsProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.GENERAL_FIELDS), generalFieldsHelp);
    }

    @Override
    public String getTabName() {
        return Localization.lang("Set General Fields");
    }

    @Override
    public void setValues() {
        viewModel.setValues();
    }

    @FXML
    void resetToDefaults() {
        viewModel.resetToDefaults();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }
}
