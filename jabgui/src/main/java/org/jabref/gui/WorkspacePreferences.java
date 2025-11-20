package org.jabref.gui;

import java.util.List;
import java.util.Locale;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.theme.Theme;
import org.jabref.logic.l10n.Language;

public class WorkspacePreferences {
    private final ObjectProperty<Language> language;
    private final BooleanProperty shouldOverrideDefaultFontSize;
    private final IntegerProperty mainFontSize;
    private final IntegerProperty defaultFontSize;
    private final ObjectProperty<Theme> theme;
    private final BooleanProperty themeSyncOs;
    private final BooleanProperty shouldOpenLastEdited;
    private final BooleanProperty showAdvancedHints;
    private final BooleanProperty confirmDelete;
    private final BooleanProperty confirmHideTabBar;
    private final ObservableList<String> selectedSlrCatalogs;

    public WorkspacePreferences(Language language,
                                boolean shouldOverrideDefaultFontSize,
                                int mainFontSize,
                                int defaultFontSize,
                                Theme theme,
                                boolean themeSyncOs,
                                boolean shouldOpenLastEdited,
                                boolean showAdvancedHints,
                                boolean confirmDelete,
                                boolean confirmHideTabBar,
                                List<String> selectedSlrCatalogs) {
        this.language = new SimpleObjectProperty<>(language);
        this.shouldOverrideDefaultFontSize = new SimpleBooleanProperty(shouldOverrideDefaultFontSize);
        this.mainFontSize = new SimpleIntegerProperty(mainFontSize);
        this.defaultFontSize = new SimpleIntegerProperty(defaultFontSize);
        this.theme = new SimpleObjectProperty<>(theme);
        this.themeSyncOs = new SimpleBooleanProperty(themeSyncOs);
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
                9,                                                          // FixMe: main default and default default is weird
                new Theme(Theme.BASE_CSS),                                  // Default theme
                false,                                                      // Default theme sync with OS
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
        this.defaultFontSize.set(preferences.getDefaultFontSize());
        this.theme.set(preferences.getTheme());
        this.themeSyncOs.set(preferences.shouldThemeSyncOs());
        this.shouldOpenLastEdited.set(preferences.shouldOpenLastEdited());
        this.showAdvancedHints.set(preferences.shouldShowAdvancedHints());
        this.confirmDelete.set(preferences.shouldConfirmDelete());
        this.confirmHideTabBar.set(preferences.shouldHideTabBar());
        this.selectedSlrCatalogs.setAll(preferences.getSelectedSlrCatalogs());
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

    public int getDefaultFontSize() {
        return defaultFontSize.get();
    }

    public void setMainFontSize(int mainFontSize) {
        this.mainFontSize.set(mainFontSize);
    }

    public IntegerProperty mainFontSizeProperty() {
        return mainFontSize;
    }

    public Theme getTheme() {
        return theme.get();
    }

    public void setTheme(Theme theme) {
        this.theme.set(theme);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public boolean shouldThemeSyncOs() {
        return themeSyncOs.get();
    }

    public BooleanProperty themeSyncOsProperty() {
        return themeSyncOs;
    }

    public void setThemeSyncOs(boolean themeSyncOs) {
        this.themeSyncOs.set(themeSyncOs);
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
}
