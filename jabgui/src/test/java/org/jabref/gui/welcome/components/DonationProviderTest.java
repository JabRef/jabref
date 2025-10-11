package org.jabref.gui.welcome.components;

import java.time.LocalDate;

import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DonationProviderTest {
    @Test
    public void calculateDaysUntilNextPopup() {
        DonationProvider donationProvider = new DonationProvider(new StackPane(), mock(GuiPreferences.class), mock(DialogService.class));

        int lastShownEpochDay = (int) LocalDate.now().minusDays(400).toEpochDay();
        int daysUntilNextPopup = donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay);
        assertEquals(0, daysUntilNextPopup);

        lastShownEpochDay = (int) LocalDate.now().toEpochDay();
        daysUntilNextPopup = donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay);
        assertEquals(365, daysUntilNextPopup);
    }
}
