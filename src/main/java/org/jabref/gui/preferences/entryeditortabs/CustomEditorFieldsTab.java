package org.jabref.gui.preferences.entryeditortabs;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomEditorFieldsTab extends AbstractPreferenceTabView<CustomEditorFieldsTabViewModel> implements PreferencesTab {

    @FXML private Button generalFieldsHelp;
    @FXML private TextArea fieldsTextArea;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;

    public CustomEditorFieldsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Custom editor tabs");
    }

    public void initialize() {
        viewModel = new CustomEditorFieldsTabViewModel(dialogService, preferences);

        fieldsTextArea.textProperty().bindBidirectional(viewModel.fieldsProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.GENERAL_FIELDS), generalFieldsHelp);
    }

    @FXML
    void resetToDefaults() {
        viewModel.resetToDefaults();
    }
}
