package org.jabref.gui.welcome;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DonationPreferences {
    private final IntegerProperty lastShownEpochDay = new SimpleIntegerProperty();

    public DonationPreferences(int lastShownEpochDay) {
        this.lastShownEpochDay.set(lastShownEpochDay);
    }

    ///  Creates object with default values
    private DonationPreferences() {
        this(-1); // Donation last shown epoch day
    }

    public static DonationPreferences getDefault() {
        return new DonationPreferences();
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
