package org.jabref.gui.preferences;

import java.util.Objects;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class SimplePreferencesDialog extends BaseDialog<SimplePreferencesDialogViewModel> {
    @FXML private ScrollPane preferencesContainer;
    @FXML private ButtonType saveButton;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;

    private final PreferencesTab preferencesTab;

    private SimplePreferencesDialogViewModel viewModel;

    public SimplePreferencesDialog(PreferencesTab preferencesTab) {
        this.preferencesTab = Objects.requireNonNull(preferencesTab);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> saveAndClose());
    }

    @FXML
    private void initialize() {
        viewModel = new SimplePreferencesDialogViewModel(dialogService, preferencesService, preferencesTab);
        viewModel.setValues();

        preferencesContainer.setContent(preferencesTab.getBuilder());
        ((AbstractPreferenceTabView<?>) preferencesTab).prefWidthProperty().bind(preferencesContainer.widthProperty());
    }

    private void saveAndClose() {
        if (viewModel.validSettings()) {
            viewModel.storeSettings();
            close();
        }
    }
}
