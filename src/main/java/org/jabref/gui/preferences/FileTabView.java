package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class FileTabView extends VBox implements PrefsTab {

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    private FileTabViewModel viewModel;

    public FileTabView (DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new FileTabViewModel(dialogService, preferences);
    }

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {
        // Done by bindings
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Deprecated
    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }
}
