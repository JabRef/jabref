package org.jabref.gui.preferences;

import java.util.List;

import javax.inject.Inject;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

public abstract class AbstractPreferenceTabView<T extends PreferenceTabViewModel> extends VBox implements PreferencesTab {

    @Inject protected TaskExecutor taskExecutor;
    @Inject protected DialogService dialogService;
    @Inject protected PreferencesService preferencesService;

    protected T viewModel;

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {
        viewModel.setValues();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return viewModel.getRestartWarnings();
    }
}
