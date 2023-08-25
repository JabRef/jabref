package org.jabref.logic.journals;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.util.EnablementStatus;

public class JournalInformationPreferences {
    private final ObjectProperty<EnablementStatus> enablementStatus;

    public JournalInformationPreferences(EnablementStatus enablementStatus) {
        this.enablementStatus = new SimpleObjectProperty<>(enablementStatus);
    }

    public EnablementStatus getEnablementStatus() {
        return enablementStatus.get();
    }

    public ObjectProperty<EnablementStatus> enablementStatusProperty() {
        return enablementStatus;
    }

    public void setEnablementStatus(EnablementStatus enablementStatus) {
        this.enablementStatus.set(enablementStatus);
    }
}
