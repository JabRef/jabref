package org.jabref.preferences;

public class MrDlibPreferences {

    private final boolean acceptRecommendations;
    private final boolean shouldSendLanguage;
    private final boolean shouldSendOs;
    private final boolean shouldSendTimezone;

    public MrDlibPreferences(boolean acceptRecommendations, boolean shouldSendLanguage, boolean shouldSendOs, boolean shouldSendTimezone) {
        this.acceptRecommendations = acceptRecommendations;
        this.shouldSendLanguage = shouldSendLanguage;
        this.shouldSendOs = shouldSendOs;
        this.shouldSendTimezone = shouldSendTimezone;
    }

    public boolean shouldAcceptRecommendations() {
        return acceptRecommendations;
    }

    public boolean shouldSendLanguage() {
        return shouldSendLanguage;
    }

    public boolean shouldSendOs() {
        return shouldSendOs;
    }

    public boolean shouldSendTimezone() {
        return shouldSendTimezone;
    }
}
