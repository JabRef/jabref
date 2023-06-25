package org.jabref.logic.journals;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class JournalInformationPreferences {
    private final BooleanProperty journalInfoEnabled;
    private final BooleanProperty journalInfoOptOut;

    public JournalInformationPreferences(boolean journalInfoEnabled, boolean journalInfoOptOut) {
        this.journalInfoEnabled = new SimpleBooleanProperty(journalInfoEnabled);
        this.journalInfoOptOut = new SimpleBooleanProperty(journalInfoOptOut);
    }

    public boolean isJournalInfoEnabled() {
        return journalInfoEnabled.get();
    }

    public BooleanProperty journalInfoEnabledProperty() {
        return journalInfoEnabled;
    }

    public void setJournalInfoEnabled(boolean journalInfoEnabled) {
        this.journalInfoEnabled.set(journalInfoEnabled);
    }

    public boolean isJournalInfoOptOut() {
        return journalInfoOptOut.get();
    }

    public BooleanProperty journalInfoOptOutProperty() {
        return journalInfoOptOut;
    }

    public void setJournalInfoOptOut(boolean journalInfoOptOut) {
        this.journalInfoOptOut.set(journalInfoOptOut);
    }
}
