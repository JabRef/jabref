package org.jabref.gui;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;

public interface LibraryTabContainer {
    LibraryTab getLibraryTabAt(int i);

    List<LibraryTab> getLibraryTabs();

    LibraryTab getCurrentLibraryTab();

    void showLibraryTab(LibraryTab libraryTab);

    void addTab(LibraryTab libraryTab, boolean raisePanel);

    LibraryTab addTab(BibDatabaseContext databaseContext, boolean raisePanel);

    void closeTab(LibraryTab libraryTab);

    void closeCurrentTab();
}
