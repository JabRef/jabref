package org.jabref.gui.preferences.file;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.NewLineSeparator;

import com.airhacks.afterburner.views.ViewLoader;

public class FileTab extends AbstractPreferenceTabView<FileTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openLastStartup;
    @FXML private TextField noWrapFiles;
    @FXML private RadioButton resolveStringsBibTex;
    @FXML private RadioButton resolveStringsAll;
    @FXML private TextField resolveStringsExcept;
    @FXML private ComboBox<NewLineSeparator> newLineSeparator;
    @FXML private CheckBox alwaysReformatBib;

    @FXML private SaveOrderConfigPanel exportOrderPanel;

    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;

    public FileTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new FileTabViewModel(preferencesService);
        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        noWrapFiles.textProperty().bindBidirectional(viewModel.noWrapFilesProperty());
        resolveStringsBibTex.selectedProperty().bindBidirectional(viewModel.resolveStringsBibTexProperty());
        resolveStringsAll.selectedProperty().bindBidirectional(viewModel.resolveStringsAllProperty());
        resolveStringsExcept.textProperty().bindBidirectional(viewModel.resolvStringsExceptProperty());
        resolveStringsExcept.disableProperty().bind(resolveStringsAll.selectedProperty().not());
        new ViewModelListCellFactory<NewLineSeparator>()
                .withText(NewLineSeparator::getDisplayName)
                .install(newLineSeparator);
        newLineSeparator.itemsProperty().bind(viewModel.newLineSeparatorListProperty());
        newLineSeparator.valueProperty().bindBidirectional(viewModel.selectedNewLineSeparatorProperty());
        alwaysReformatBib.selectedProperty().bindBidirectional(viewModel.alwaysReformatBibProperty());

        exportOrderPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportOrderPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportOrderPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        exportOrderPanel.primarySortFieldsProperty().bind(viewModel.primarySortFieldsProperty());
        exportOrderPanel.secondarySortFieldsProperty().bind(viewModel.secondarySortFieldsProperty());
        exportOrderPanel.tertiarySortFieldsProperty().bind(viewModel.tertiarySortFieldsProperty());
        exportOrderPanel.savePrimaryDescPropertySelected().bindBidirectional(viewModel.savePrimaryDescPropertySelected());
        exportOrderPanel.saveSecondaryDescPropertySelected().bindBidirectional(viewModel.saveSecondaryDescPropertySelected());
        exportOrderPanel.saveTertiaryDescPropertySelected().bindBidirectional(viewModel.saveTertiaryDescPropertySelected());
        exportOrderPanel.savePrimarySortSelectedValueProperty().bindBidirectional(viewModel.savePrimarySortSelectedValueProperty());
        exportOrderPanel.saveSecondarySortSelectedValueProperty().bindBidirectional(viewModel.saveSecondarySortSelectedValueProperty());
        exportOrderPanel.saveTertiarySortSelectedValueProperty().bindBidirectional(viewModel.saveTertiarySortSelectedValueProperty());

        autosaveLocalLibraries.selectedProperty().bindBidirectional(viewModel.autosaveLocalLibrariesProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE), autosaveLocalLibrariesHelp);
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }
}
