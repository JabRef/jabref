package org.jabref.gui;

import java.util.List;

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

    /// Ids of the remembered shared databases that are still shown as a placeholder tab.
    /// They have no library tab, but must stay remembered for the next session.
    List<String> getUnconnectedSharedDatabaseIds();

    /// Refreshes the ui after changes to the preferences
    void refresh();
}
