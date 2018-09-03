package org.jabref.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class EntryTypeViewModel {

    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();

    public BooleanProperty searchingProperty() {
        return searchingProperty;
    }
}
