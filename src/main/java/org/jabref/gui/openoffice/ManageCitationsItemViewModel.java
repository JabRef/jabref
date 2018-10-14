package org.jabref.gui.openoffice;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ManageCitationsItemViewModel {

    private final StringProperty citation = new SimpleStringProperty("");
    private final StringProperty extraInformation = new SimpleStringProperty("");

    public StringProperty citationProperty() {
        return citation;
    }

    public StringProperty extraInformationProperty() {
        return extraInformation;
    }

}
