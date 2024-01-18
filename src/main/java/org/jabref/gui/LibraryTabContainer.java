package org.jabref.gui;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;

public interface LibraryTabContainer {
    List<LibraryTab> getLibraryTabs();

    LibraryTab getCurrentLibraryTab();

    void showLibraryTab(LibraryTab libraryTab);

    void addTab(LibraryTab libraryTab, boolean raisePanel);

    void addTab(BibDatabaseContext bibDatabaseContext, boolean raisePanel);

    /**
     * Closes a designated libraryTab
     *
     * @param libraryTab to be closed.
     * @return true if closing the tab was successful
     */
    boolean closeTab(LibraryTab libraryTab);

    /**
     * Closes the currently viewed libraryTab
     *
     * @return true if closing the tab was successful
     */
    boolean closeCurrentTab();

    /**
     * Refreshes the ui after changes to the preferences
     */
    void refresh();
}
