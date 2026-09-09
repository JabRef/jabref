package org.jabref.gui.welcome;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DonationPreferences {
    private final IntegerProperty nextNotificationEpochDay = new SimpleIntegerProperty();

    public DonationPreferences(int nextNotificationEpochDay) {
        this.nextNotificationEpochDay.set(nextNotificationEpochDay);
    }

    ///  Creates object with default values
    private DonationPreferences() {
        this(-1); // No donation notification scheduled yet
    }

    public static DonationPreferences getDefault() {
        return new DonationPreferences();
    }

    public int getNextNotificationEpochDay() {
        return nextNotificationEpochDay.get();
    }

    public void setNextNotificationEpochDay(int value) {
        this.nextNotificationEpochDay.set(value);
    }

    public IntegerProperty nextNotificationEpochDayProperty() {
        return nextNotificationEpochDay;
    }
}
