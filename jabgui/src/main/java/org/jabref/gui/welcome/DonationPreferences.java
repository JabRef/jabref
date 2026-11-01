package org.jabref.gui.welcome;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DonationPreferences {
    private final BooleanProperty neverShowAgain = new SimpleBooleanProperty();
    private final IntegerProperty nextNotificationEpochDay = new SimpleIntegerProperty();

    public DonationPreferences(boolean neverShowAgain, int nextNotificationEpochDay) {
        this.neverShowAgain.set(neverShowAgain);
        this.nextNotificationEpochDay.set(nextNotificationEpochDay);
    }

    ///  Creates object with default values
    private DonationPreferences() {
        this(
                false,                    // Donation never show again
                -1);                      // No donation notification scheduled yet
    }

    public static DonationPreferences getDefault() {
        return new DonationPreferences();
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
