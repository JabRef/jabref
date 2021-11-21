package org.jabref.gui.libraryproperties.saving;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class SavingPropertiesView extends AbstractPropertiesTabView<SavingPropertiesViewModel> implements PropertiesTab {

    @FXML private CheckBox protect;
    @FXML private SaveOrderConfigPanel saveOrderConfigPanel;
    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;

    @Inject private PreferencesService preferencesService;

    public SavingPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Saving");
    }

    public void initialize() {
        this.viewModel = new SavingPropertiesViewModel(databaseContext, preferencesService);

        protect.disableProperty().bind(viewModel.protectDisableProperty());
        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());

        saveOrderConfigPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        saveOrderConfigPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        saveOrderConfigPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        saveOrderConfigPanel.sortableFieldsProperty().bind(viewModel.sortableFieldsProperty());
        saveOrderConfigPanel.sortCriteriaProperty().bindBidirectional(viewModel.sortCriteriaProperty());

        fieldFormatterCleanupsPanel.cleanupsDisableProperty().bindBidirectional(viewModel.cleanupsDisableProperty());
        fieldFormatterCleanupsPanel.cleanupsProperty().bindBidirectional(viewModel.cleanupsProperty());
    }
}
