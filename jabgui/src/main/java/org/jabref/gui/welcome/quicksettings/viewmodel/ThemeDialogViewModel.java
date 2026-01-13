package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.StandardFileType;

public class ThemeDialogViewModel extends AbstractViewModel {

    private final ObjectProperty<ThemeTypes> selectedThemeProperty = new SimpleObjectProperty<>();
    private final StringProperty customPathProperty = new SimpleStringProperty("");

    private final WorkspacePreferences workspacePreferences;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public ThemeDialogViewModel(GuiPreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.workspacePreferences = preferences.getWorkspacePreferences();
        this.dialogService = dialogService;

        initializeFromCurrentTheme();
    }

    private void initializeFromCurrentTheme() {
        Theme currentTheme = workspacePreferences.getTheme();
        switch (currentTheme.getType()) {
            case DEFAULT ->
                    selectedThemeProperty.set(ThemeTypes.LIGHT);
            case EMBEDDED ->
                    selectedThemeProperty.set(ThemeTypes.DARK);
            case CUSTOM -> {
                selectedThemeProperty.set(ThemeTypes.CUSTOM);
                customPathProperty.set(currentTheme.getName());
            }
        }
    }

    public ThemeTypes getSelectedTheme() {
        return selectedThemeProperty.get();
    }

    public void setSelectedTheme(ThemeTypes theme) {
        selectedThemeProperty.set(theme);
    }

    public StringProperty customPathProperty() {
        return customPathProperty;
    }

    public void setCustomPath(String path) {
        customPathProperty.set(path);
    }

    public void browseForThemeFile() {
        String fileDir = customPathProperty.get().isEmpty() ?
                         preferences.getInternalPreferences().getLastPreferencesExportPath().toString() :
                         customPathProperty.get();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(fileDir)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> setCustomPath(file.toAbsolutePath().toString()));
    }

    public boolean isValidConfiguration() {
        if (selectedThemeProperty.get() == ThemeTypes.CUSTOM) {
            return !customPathProperty.get().trim().isEmpty() && Files.exists(Path.of(customPathProperty.get()));
        }
        return selectedThemeProperty.get() != null;
    }

    public void saveSettings() {
        Theme newTheme = switch (selectedThemeProperty.get()) {
            case LIGHT ->
                    Theme.light();
            case DARK ->
                    Theme.dark();
            case CUSTOM ->
                    Theme.custom(customPathProperty.get().trim());
        };
        workspacePreferences.setTheme(newTheme);
    }
}
