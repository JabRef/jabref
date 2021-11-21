package org.jabref.gui.newlibraryproperties;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesView extends BaseDialog<LibraryPropertiesViewModel> {

    @FXML private ButtonType saveButton;

    @Inject private DialogService dialogService;

    private final LibraryTab libraryTab;
    private LibraryPropertiesViewModel viewModel;

    public LibraryPropertiesView(LibraryTab libraryTab) {
        this.libraryTab = libraryTab;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> savePreferencesAndCloseDialog());

        setTitle(Localization.lang("Library properties"));
    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesViewModel(libraryTab);

        viewModel.setValues();
    }

    private void savePreferencesAndCloseDialog() {
        viewModel.storeAllSettings();
        close();
    }
}
