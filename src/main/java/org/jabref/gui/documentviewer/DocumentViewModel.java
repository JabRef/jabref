package org.jabref.gui.documentviewer;

import javafx.collections.ObservableList;

public abstract class DocumentViewModel {
    public abstract ObservableList<DocumentPageViewModel> getPages();
}
