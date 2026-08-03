package org.jabref.gui.openoffice;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.openoffice.style.CitationType;

public class CiteSpecialDialogViewModel {

    private final StringProperty pageInfo = new SimpleStringProperty("");
    private final ObjectProperty<CitationType> citationType;

    public CiteSpecialDialogViewModel(CitationType citationType) {
        this.citationType = new SimpleObjectProperty<>(citationType);
    }

    public StringProperty pageInfoProperty() {
        return pageInfo;
    }

    public ObjectProperty<CitationType> citationTypeProperty() {
        return citationType;
    }
}
