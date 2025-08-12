package org.jabref.gui.welcome.components;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DonationProviderTest {
    @Test
    public void testCalculateDaysUntilNextPopup() {
        DonationProvider donationProvider = new DonationProvider(null, null, null);

        int lastShownEpochDay = (int) LocalDate.now().minusDays(400).toEpochDay();
        int daysUntilNextPopup = donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay);
        assertEquals(0, daysUntilNextPopup);

        lastShownEpochDay = (int) LocalDate.now().toEpochDay();
        daysUntilNextPopup = donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay);
        assertEquals(365, daysUntilNextPopup);
    }
}
