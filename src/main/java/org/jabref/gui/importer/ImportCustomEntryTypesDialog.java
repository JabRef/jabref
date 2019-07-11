package org.jabref.gui.importer;

import java.util.List;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.CheckListView;

public class ImportBibEntryTypesDialog extends BaseDialog<Void> {

    @FXML private CheckListView<EntryType> unknownEntryTypesCheckList;
    @FXML private VBox boxDifferentCustomization;
    @FXML private CheckListView<EntryType> differentCustomizationCheckList;
    @Inject private PreferencesService preferencesService;

    private final List<EntryType> BibEntryTypes;

    private final BibDatabaseMode mode;
    private ImportBibEntryTypesDialogViewModel viewModel;

    public ImportBibEntryTypesDialog(BibDatabaseMode mode, List<EntryType> BibEntryTypes) {
        this.mode = mode;
        this.BibEntryTypes = BibEntryTypes;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                viewModel.importBibEntryTypes(unknownEntryTypesCheckList.getCheckModel().getCheckedItems(), differentCustomizationCheckList.getCheckModel().getCheckedItems());
            }
            return null;
        });

        setTitle(Localization.lang("Custom entry types"));

    }

    @FXML
    public void initialize() {
        viewModel = new ImportBibEntryTypesDialogViewModel(mode, BibEntryTypes, preferencesService);

        boxDifferentCustomization.managedProperty().bind(Bindings.isNotEmpty(viewModel.differentCustomizations()));
        unknownEntryTypesCheckList.setItems(viewModel.newTypes());
        differentCustomizationCheckList.setItems(viewModel.differentCustomizations());
    }

}
