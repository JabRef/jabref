package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MrDlibPreferences {

    private final BooleanProperty acceptRecommendations;
    private final BooleanProperty sendLanguage;
    private final BooleanProperty sendOs;
    private final BooleanProperty sendTimezone;

    public MrDlibPreferences(boolean acceptRecommendations, boolean shouldSendLanguage, boolean shouldSendOs, boolean shouldSendTimezone) {
        this.acceptRecommendations = new SimpleBooleanProperty(acceptRecommendations);
        this.sendLanguage = new SimpleBooleanProperty(shouldSendLanguage);
        this.sendOs = new SimpleBooleanProperty(shouldSendOs);
        this.sendTimezone = new SimpleBooleanProperty(shouldSendTimezone);
    }

    public boolean shouldAcceptRecommendations() {
        return acceptRecommendations.get();
    }

    public BooleanProperty acceptRecommendationsProperty() {
        return acceptRecommendations;
    }

    public void setAcceptRecommendations(boolean acceptRecommendations) {
        this.acceptRecommendations.set(acceptRecommendations);
    }

    public boolean shouldSendLanguage() {
        return sendLanguage.get();
    }

    public BooleanProperty sendLanguageProperty() {
        return sendLanguage;
    }

    public void setSendLanguage(boolean sendLanguage) {
        this.sendLanguage.set(sendLanguage);
    }

    public boolean shouldSendOs() {
        return sendOs.get();
    }

    public BooleanProperty sendOsProperty() {
        return sendOs;
    }

    public void setSendOs(boolean sendOs) {
        this.sendOs.set(sendOs);
    }

    public boolean shouldSendTimezone() {
        return sendTimezone.get();
    }

    public BooleanProperty sendTimezoneProperty() {
        return sendTimezone;
    }

    public void setSendTimezone(boolean sendTimezone) {
        this.sendTimezone.set(sendTimezone);
    }
}
