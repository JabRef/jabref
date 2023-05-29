package org.jabref.gui.preferences.entryeditor;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class EntryEditorTab extends AbstractPreferenceTabView<EntryEditorTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openOnNewEntry;
    @FXML private CheckBox defaultSource;
    @FXML private CheckBox enableRelatedArticlesTab;
    @FXML private CheckBox acceptRecommendations;
    @FXML private CheckBox enableLatexCitationsTab;
    @FXML private CheckBox enableValidation;
    @FXML private CheckBox allowIntegerEdition;
    @FXML private CheckBox autoLinkFilesEnabled;

    @FXML private Button generalFieldsHelp;
    @FXML private TextArea fieldsTextArea;

    @Inject private KeyBindingRepository keyBindingRepository;

    public EntryEditorTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }

    public void initialize() {
        this.viewModel = new EntryEditorTabViewModel(dialogService, preferencesService);

        openOnNewEntry.selectedProperty().bindBidirectional(viewModel.openOnNewEntryProperty());
        defaultSource.selectedProperty().bindBidirectional(viewModel.defaultSourceProperty());
        enableRelatedArticlesTab.selectedProperty().bindBidirectional(viewModel.enableRelatedArticlesTabProperty());
        acceptRecommendations.selectedProperty().bindBidirectional(viewModel.acceptRecommendationsProperty());
        enableLatexCitationsTab.selectedProperty().bindBidirectional(viewModel.enableLatexCitationsTabProperty());
        enableValidation.selectedProperty().bindBidirectional(viewModel.enableValidationProperty());
        allowIntegerEdition.selectedProperty().bindBidirectional(viewModel.allowIntegerEditionProperty());
        autoLinkFilesEnabled.selectedProperty().bindBidirectional(viewModel.autoLinkFilesEnabledProperty());

        fieldsTextArea.textProperty().bindBidirectional(viewModel.fieldsProperty());

        ActionFactory actionFactory = new ActionFactory(keyBindingRepository);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.GENERAL_FIELDS, dialogService), generalFieldsHelp);
    }

    @FXML
    void resetToDefaults() {
        viewModel.resetToDefaults();
    }
}
