package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.StyleSheet;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.strings.StringUtil;

public class ThemeDialogViewModel extends AbstractViewModel {

    private final ReadOnlyListProperty<ThemePreset> themesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemePreset.values()));
    private final ObjectProperty<ThemePreset> selectedThemeProperty = new SimpleObjectProperty<>();

    private final ReadOnlyListProperty<ThemeColorScheme> colorSchemeListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemeColorScheme.values()));
    private final ObjectProperty<ThemeColorScheme> selectedThemeColorSchemeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty customThemeEnabled = new SimpleBooleanProperty(false);
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty("");

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
        selectedThemeProperty.set(workspacePreferences.getTheme());
        selectedThemeColorSchemeProperty.set(workspacePreferences.getColorScheme());
        customThemeEnabled.setValue(workspacePreferences.getCustomTheme().isPresent());
        customPathToThemeProperty.setValue(workspacePreferences.getCustomTheme().map(StyleSheet::getName).orElse(""));
    }

    public ReadOnlyListProperty<ThemePreset> themesListProperty() {
        return this.themesListProperty;
    }

    public ObjectProperty<ThemePreset> selectedThemeProperty() {
        return selectedThemeProperty;
    }

    public ReadOnlyListProperty<ThemeColorScheme> colorSchemeListProperty() {
        return colorSchemeListProperty;
    }

    public ObjectProperty<ThemeColorScheme> selectedThemeColorSchemeProperty() {
        return selectedThemeColorSchemeProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public boolean isValidConfiguration() {
        return getSelectedTheme() != null && getSelectedThemeColorScheme() != null;
    }

    public void saveSettings() {
        workspacePreferences.setTheme(getSelectedTheme());
        workspacePreferences.setColorScheme(getSelectedThemeColorScheme());

        String customTheme = customPathToThemeProperty.getValue();
        if (customThemeEnabled.get() && !StringUtil.isBlank(customTheme)) {
            workspacePreferences.setCustomTheme(StyleSheet.create(customTheme));
        } else {
            workspacePreferences.setCustomTheme(Optional.empty());
        }
    }

    public void importCSSFile() {
        String fileDir = customPathToThemeProperty.getValue().isEmpty() ? preferences.getInternalPreferences().getLastPreferencesExportPath().toString()
                                                                        : customPathToThemeProperty.getValue();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(fileDir).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file ->
                customPathToThemeProperty.setValue(file.toAbsolutePath().toString()));
    }

    private ThemePreset getSelectedTheme() {
        return selectedThemeProperty.get();
    }

    private ThemeColorScheme getSelectedThemeColorScheme() {
        return selectedThemeColorSchemeProperty.get();
    }

    public BooleanProperty customThemeEnabledProperty() {
        return customThemeEnabled;
    }
}
