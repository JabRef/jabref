package org.jabref.gui;

import java.util.List;

public interface LibraryTabContainer {
    LibraryTab getLibraryTabAt(int i);

    List<LibraryTab> getLibraryTabs();

    LibraryTab getCurrentLibraryTab();

    void showLibraryTab(LibraryTab libraryTab);

    void addTab(LibraryTab libraryTab, boolean raisePanel);

    void closeTab(LibraryTab libraryTab);

    void closeCurrentTab();
}
