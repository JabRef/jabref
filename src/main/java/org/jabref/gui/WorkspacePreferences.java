package org.jabref.gui;

import java.util.List;

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
    private final BooleanProperty warnAboutDuplicatesInInspection;
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
                                boolean warnAboutDuplicatesInInspection,
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
        this.warnAboutDuplicatesInInspection = new SimpleBooleanProperty(warnAboutDuplicatesInInspection);
        this.confirmDelete = new SimpleBooleanProperty(confirmDelete);
        this.confirmHideTabBar = new SimpleBooleanProperty(confirmHideTabBar);
        this.selectedSlrCatalogs = FXCollections.observableArrayList(selectedSlrCatalogs);
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

    public boolean shouldWarnAboutDuplicatesInInspection() {
        return warnAboutDuplicatesInInspection.get();
    }

    public BooleanProperty warnAboutDuplicatesInInspectionProperty() {
        return warnAboutDuplicatesInInspection;
    }

    public void setWarnAboutDuplicatesInInspection(boolean warnAboutDuplicatesInInspection) {
        this.warnAboutDuplicatesInInspection.set(warnAboutDuplicatesInInspection);
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

    public BooleanProperty confirmHideTabBarProperty() {
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
