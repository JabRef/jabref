package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.gui.shared.SharedDatabasePlaceholderTab;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface LibraryTabContainer {
    ObservableList<LibraryTab> getLibraryTabs();

    @Nullable
    LibraryTab getCurrentLibraryTab();

    void showLibraryTab(LibraryTab libraryTab);

    void addTab(BibDatabaseContext bibDatabaseContext, boolean raisePanel);

    void addTab(LibraryTab libraryTab, boolean raisePanel);

    /// Closes a designated libraryTab
    ///
    /// @param tab to be closed.
    /// @return true if closing the tab was successful
    boolean closeTab(@Nullable LibraryTab tab);

    /// Closes the designated libraryTabs
    ///
    /// @param tabs           to be closed.
    /// @param showWelcomeTab whether to show the welcome tab if no library tab remains open afterwards (should be false when quitting the application)
    /// @return true if closing the tabs was successful
    boolean closeTabs(List<LibraryTab> tabs, boolean showWelcomeTab);

    /// Shows a placeholder for a shared database that is being connected (or failed to)
    void showSharedDatabasePlaceholder(SharedDatabasePlaceholderTab placeholder, boolean raisePanel);

    /// Shared databases that are being connected or failed to, i.e. shown as a placeholder instead of a library tab
    List<SharedDatabasePlaceholderTab> getSharedDatabasePlaceholders();

    /// Ids of the remembered shared databases that are still shown as a placeholder tab.
    /// They have no library tab, but must stay remembered for the next session.
    default List<String> getUnconnectedSharedDatabaseIds() {
        return getSharedDatabasePlaceholders().stream()
                                              .map(SharedDatabasePlaceholderTab::getSharedDatabaseId)
                                              .flatMap(Optional::stream)
                                              .toList();
    }

    /// Refreshes the ui after changes to the preferences
    void refresh();
}
