package org.jabref.gui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.theme.StyleSheet;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.util.OptionalObjectProperty;

public class WorkspacePreferences {
    private final ObjectProperty<Language> language;
    private final BooleanProperty shouldOverrideDefaultFontSize;
    private final IntegerProperty mainFontSize;
    private final ObjectProperty<ThemePreset> theme;
    private final ObjectProperty<ThemeColorScheme> colorScheme;
    private final OptionalObjectProperty<StyleSheet> customTheme;

    private final BooleanProperty shouldOpenLastEdited;
    private final BooleanProperty showAdvancedHints;
    private final BooleanProperty confirmDelete;
    private final BooleanProperty confirmHideTabBar;
    private final ObservableList<String> selectedSlrCatalogs;

    public WorkspacePreferences(Language language,
                                boolean shouldOverrideDefaultFontSize,
                                int mainFontSize,
                                ThemePreset theme,
                                ThemeColorScheme colorScheme,
                                StyleSheet customTheme,
                                boolean shouldOpenLastEdited,
                                boolean showAdvancedHints,
                                boolean confirmDelete,
                                boolean confirmHideTabBar,
                                List<String> selectedSlrCatalogs) {
        this.language = new SimpleObjectProperty<>(language);
        this.shouldOverrideDefaultFontSize = new SimpleBooleanProperty(shouldOverrideDefaultFontSize);
        this.mainFontSize = new SimpleIntegerProperty(mainFontSize);
        this.theme = new SimpleObjectProperty<>(theme);
        this.colorScheme = new SimpleObjectProperty<>(colorScheme);
        this.customTheme = OptionalObjectProperty.empty();
        this.customTheme.set(Optional.ofNullable(customTheme));
        this.shouldOpenLastEdited = new SimpleBooleanProperty(shouldOpenLastEdited);
        this.showAdvancedHints = new SimpleBooleanProperty(showAdvancedHints);
        this.confirmDelete = new SimpleBooleanProperty(confirmDelete);
        this.confirmHideTabBar = new SimpleBooleanProperty(confirmHideTabBar);
        this.selectedSlrCatalogs = FXCollections.observableArrayList(selectedSlrCatalogs);
    }

    /// Creates Object with default values
    private WorkspacePreferences() {
        this(
                Language.getLanguageFor(Locale.getDefault().getLanguage()), // Default language
                false,                                                      // Default font size override
                9,                                                          // Default font size
                ThemePreset.JABREF,                                         // Default theme
                ThemeColorScheme.FOLLOW_SYSTEM,                             // Default color scheme is follow system
                null,                                                       // Custom theme
                true,                                                       // Default open last edited
                true,                                                       // Default show advanced hints
                true,                                                       // Default confirm delete
                true,                                                       // Default confirm hide tab bar
                List.of()                                                   // Default selected SLR catalogs
        );
    }

    public static WorkspacePreferences getDefault() {
        return new WorkspacePreferences();
    }

    public void setAll(WorkspacePreferences preferences) {
        this.language.set(preferences.getLanguage());
        this.shouldOverrideDefaultFontSize.set(preferences.shouldOverrideDefaultFontSize());
        this.mainFontSize.set(preferences.getMainFontSize());
        this.theme.set(preferences.getTheme());
        this.colorScheme.set(preferences.getColorScheme());
        this.shouldOpenLastEdited.set(preferences.shouldOpenLastEdited());
        this.showAdvancedHints.set(preferences.shouldShowAdvancedHints());
        this.confirmDelete.set(preferences.shouldConfirmDelete());
        this.confirmHideTabBar.set(preferences.shouldHideTabBar());
        this.selectedSlrCatalogs.setAll(preferences.getSelectedSlrCatalogs());
    }

    public ThemeColorScheme getColorScheme() {
        return colorScheme.get();
    }

    public Language getLanguage() {
        return language.get();
    }

    public ObjectProperty<Language> languageProperty() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language.set(language);
    }

    public boolean shouldOverrideDefaultFontSize() {
        return shouldOverrideDefaultFontSize.get();
    }

    public void setShouldOverrideDefaultFontSize(boolean newValue) {
        shouldOverrideDefaultFontSize.set(newValue);
    }

    public BooleanProperty shouldOverrideDefaultFontSizeProperty() {
        return shouldOverrideDefaultFontSize;
    }

    public int getMainFontSize() {
        return mainFontSize.get();
    }

    public void setMainFontSize(int mainFontSize) {
        this.mainFontSize.set(mainFontSize);
    }

    public IntegerProperty mainFontSizeProperty() {
        return mainFontSize;
    }

    public ThemePreset getTheme() {
        return theme.get();
    }

    public void setTheme(ThemePreset theme) {
        this.theme.set(theme);
    }

    public ObjectProperty<ThemePreset> themeProperty() {
        return theme;
    }

    public ThemeColorScheme shouldThemeSyncOs() {
        return colorScheme.get();
    }

    public ObjectProperty<ThemeColorScheme> colorSchemeProperty() {
        return colorScheme;
    }

    public void setColorScheme(ThemeColorScheme colorScheme) {
        this.colorScheme.set(colorScheme);
    }

    public boolean shouldOpenLastEdited() {
        return shouldOpenLastEdited.get();
    }

    public BooleanProperty openLastEditedProperty() {
        return shouldOpenLastEdited;
    }

    public void setOpenLastEdited(boolean shouldOpenLastEdited) {
        this.shouldOpenLastEdited.set(shouldOpenLastEdited);
    }

    public boolean shouldShowAdvancedHints() {
        return showAdvancedHints.get();
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return showAdvancedHints;
    }

    public void setShowAdvancedHints(boolean showAdvancedHints) {
        this.showAdvancedHints.set(showAdvancedHints);
    }

    public boolean shouldConfirmDelete() {
        return confirmDelete.get();
    }

    public BooleanProperty confirmDeleteProperty() {
        return confirmDelete;
    }

    public void setConfirmDelete(boolean confirmDelete) {
        this.confirmDelete.set(confirmDelete);
    }

    public boolean shouldHideTabBar() {
        return confirmHideTabBar.get();
    }

    public BooleanProperty hideTabBarProperty() {
        return confirmHideTabBar;
    }

    public void setHideTabBar(boolean hideTabBar) {
        this.confirmHideTabBar.set(hideTabBar);
    }

    public ObservableList<String> getSelectedSlrCatalogs() {
        return selectedSlrCatalogs;
    }

    public void setSelectedSlrCatalogs(List<String> catalogs) {
        selectedSlrCatalogs.setAll(catalogs);
    }

    public Optional<StyleSheet> getCustomTheme() {
        return customTheme.get();
    }

    public OptionalObjectProperty<StyleSheet> customThemeProperty() {
        return customTheme;
    }

    public void setCustomTheme(Optional<StyleSheet> customTheme) {
        this.customTheme.set(customTheme);
    }
}
