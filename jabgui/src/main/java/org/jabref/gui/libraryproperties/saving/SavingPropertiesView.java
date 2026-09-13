package org.jabref.gui.libraryproperties.saving;

import java.util.function.BooleanSupplier;

import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class SavingPropertiesView extends AbstractPropertiesTabView<SavingPropertiesViewModel> implements PropertiesTab {

    @FXML private CheckBox protect;
    @FXML private SaveOrderConfigPanel saveOrderConfigPanel;
    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    @FXML private ComboBox<AbbreviationType> journalAbbreviationOnSave;
    @FXML private ComboBox<Boolean> synchronizeWithFile;
    @FXML private ComboBox<Boolean> mergeConflictedCopies;

    @Inject private GuiPreferences preferences;

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
        this.viewModel = new SavingPropertiesViewModel(CleanupPreferences.getDefault().getFieldFormatterCleanups());

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

        bindOverride(synchronizeWithFile, viewModel.synchronizeWithFileProperty(), () -> preferences.getLibraryPreferences().shouldSynchronizeWithFile());
        bindOverride(mergeConflictedCopies, viewModel.mergeConflictedCopiesProperty(), () -> preferences.getLibraryPreferences().shouldMergeConflictedCopies());
    }

    /// A per-library setting that may follow the global one: `null` stands for "use global setting"
    private static void bindOverride(ComboBox<Boolean> comboBox, ObjectProperty<Boolean> property, BooleanSupplier globalSetting) {
        comboBox.setItems(FXCollections.observableArrayList(null, Boolean.TRUE, Boolean.FALSE));
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Boolean enabled) {
                if (enabled == null) {
                    return Localization.lang("Use global setting (%0)", onOrOff(globalSetting.getAsBoolean()));
                }
                return onOrOff(enabled);
            }

            @Override
            public Boolean fromString(String string) {
                return null;
            }
        });
        comboBox.valueProperty().bindBidirectional(property);
    }

    private static String onOrOff(boolean on) {
        return on ? Localization.lang("On") : Localization.lang("Off");
    }
}
