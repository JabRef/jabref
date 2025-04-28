package org.jabref.gui.documentviewer;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;

public abstract class DocumentViewModel {
    private final IntegerProperty maxPages = new SimpleIntegerProperty();

    public abstract ObservableList<DocumentPageViewModel> getPages();

    public IntegerProperty maxPagesProperty() {
        return maxPages;
    }
}
