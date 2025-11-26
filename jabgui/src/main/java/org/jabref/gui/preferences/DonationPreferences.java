package org.jabref.gui.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DonationPreferences {
    private final BooleanProperty neverShowAgain = new SimpleBooleanProperty();
    private final IntegerProperty lastShownEpochDay = new SimpleIntegerProperty();

    public DonationPreferences(boolean neverShowAgain, int lastShownEpochDay) {
        this.neverShowAgain.set(neverShowAgain);
        this.lastShownEpochDay.set(lastShownEpochDay);
    }

    ///  Creates object with default values
    private DonationPreferences() {
        this(
                false,      // Donation never show again
                -1);                      // Donation last shown epoch day
    }

    public static DonationPreferences getDefault() {
        return new DonationPreferences();
    }

    public void setAll(DonationPreferences preferences) {
        this.neverShowAgain.set(preferences.isNeverShowAgain());
        this.lastShownEpochDay.set(preferences.getLastShownEpochDay());
    }

    public boolean isNeverShowAgain() {
        return neverShowAgain.get();
    }

    public void setNeverShowAgain(boolean value) {
        this.neverShowAgain.set(value);
    }

    public BooleanProperty neverShowAgainProperty() {
        return neverShowAgain;
    }

    public int getLastShownEpochDay() {
        return lastShownEpochDay.get();
    }

    public void setLastShownEpochDay(int value) {
        this.lastShownEpochDay.set(value);
    }

    public IntegerProperty lastShownEpochDayProperty() {
        return lastShownEpochDay;
    }
}
