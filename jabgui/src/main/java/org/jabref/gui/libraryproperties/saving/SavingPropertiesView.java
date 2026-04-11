package org.jabref.gui.libraryproperties.saving;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class SavingPropertiesView extends AbstractPropertiesTabView<SavingPropertiesViewModel> implements PropertiesTab {

    @FXML private CheckBox protect;
    @FXML private SaveOrderConfigPanel saveOrderConfigPanel;
    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    @FXML private ComboBox<AbbreviationType> journalAbbreviationOnSave;

    @Inject private CliPreferences preferences;

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
        this.viewModel = new SavingPropertiesViewModel(databaseContext, preferences);

        protect.disableProperty().bind(viewModel.protectDisableProperty());
        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());

        saveOrderConfigPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        saveOrderConfigPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        saveOrderConfigPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        saveOrderConfigPanel.sortableFieldsProperty().bind(viewModel.sortableFieldsProperty());
        saveOrderConfigPanel.sortCriteriaProperty().bindBidirectional(viewModel.sortCriteriaProperty());

        fieldFormatterCleanupsPanel.cleanupsDisableProperty().bindBidirectional(viewModel.cleanupsDisableProperty());
        fieldFormatterCleanupsPanel.cleanupsProperty().bindBidirectional(viewModel.cleanupsProperty());

        journalAbbreviationOnSave.setItems(FXCollections.observableArrayList(
                null, AbbreviationType.DEFAULT, AbbreviationType.DOTLESS,
                AbbreviationType.SHORTEST_UNIQUE, AbbreviationType.LTWA));
        journalAbbreviationOnSave.setConverter(new StringConverter<>() {
            @Override
            public String toString(AbbreviationType type) {
                if (type == null) {
                    return Localization.lang("None (use global setting)");
                }
                return switch (type) {
                    case DEFAULT ->
                            Localization.lang("Abbreviate (default)");
                    case DOTLESS ->
                            Localization.lang("Abbreviate (dotless)");
                    case SHORTEST_UNIQUE ->
                            Localization.lang("Abbreviate (shortest unique)");
                    case LTWA ->
                            Localization.lang("Abbreviate (LTWA)");
                };
            }

            @Override
            public AbbreviationType fromString(String string) {
                return null;
            }
        });
        journalAbbreviationOnSave.valueProperty().bindBidirectional(viewModel.journalAbbreviationOnSaveProperty());
    }
}
