package org.jabref.gui.welcome.components;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DonationProviderTest {
    private final DonationProvider donationProvider = new DonationProvider(mock(GuiPreferences.class), mock(DialogService.class));

    @Test
    public void notificationIsDueWhenLastShownIsLongAgo() {
        int lastShownEpochDay = (int) LocalDate.now().minusYears(1).toEpochDay();
        assertEquals(0, donationProvider.calculateDaysUntilNextNotification(lastShownEpochDay));
    }

    @Test
    public void notificationIsSnoozedForSixMonthsAfterBeingShown() {
        LocalDate today = LocalDate.now();
        int expectedDays = (int) ChronoUnit.DAYS.between(today, today.plusMonths(6));
        assertEquals(expectedDays, donationProvider.calculateDaysUntilNextNotification((int) today.toEpochDay()));
    }

    @Test
    public void notificationIsShownOneWeekAfterFirstLaunch() {
        assertEquals(7, donationProvider.calculateDaysUntilNextNotification(-1));
    }
}
