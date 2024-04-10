package org.jabref.gui;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.Nullable;

public interface LibraryTabContainer {
    List<LibraryTab> getLibraryTabs();

    @Nullable
    LibraryTab getCurrentLibraryTab();

    void showLibraryTab(LibraryTab libraryTab);

    void addTab(LibraryTab libraryTab, boolean raisePanel);

    void addTab(BibDatabaseContext bibDatabaseContext, boolean raisePanel);

    /**
     * Closes a designated libraryTab
     *
     * @param tab to be closed.
     * @return true if closing the tab was successful
     */
    boolean closeTab(LibraryTab tab);

    boolean closeTabs(List<LibraryTab> tabs);

    /**
     * Refreshes the ui after changes to the preferences
     */
    void refresh();
}
