package org.jabref.gui.openoffice;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdvancedCiteDialogViewModel {

    private final StringProperty pageInfo = new SimpleStringProperty("");
    private final BooleanProperty citeInPar = new SimpleBooleanProperty();
    private final BooleanProperty citeInText = new SimpleBooleanProperty();

    public StringProperty pageInfoProperty() {
        return pageInfo;
    }

    public BooleanProperty citeInParProperty() {
        return citeInPar;
    }

    public BooleanProperty citeInTextProperty() {
        return citeInText;
    }
}
