package org.jabref.gui.welcome.components;

import java.time.LocalDate;

import org.jabref.gui.DialogService;
import org.jabref.gui.Notifications;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.welcome.DonationPreferences;

import com.dlsc.gemsfx.infocenter.Notification.OnClickBehaviour;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationProviderTest extends JavaFxTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final DonationPreferences donationPreferences = new DonationPreferences(false, -1);
    private final DonationProvider donationProvider = new DonationProvider(preferences, dialogService);

    @BeforeEach
    void setUp() {
        when(preferences.getDonationPreferences()).thenReturn(donationPreferences);
    }

    @Test
    public void calculateDaysUntilNextPopup() {
        int lastShownEpochDay = (int) LocalDate.now().minusDays(400).toEpochDay();
        assertEquals(0, donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay));

        lastShownEpochDay = (int) LocalDate.now().toEpochDay();
        assertEquals(365, donationProvider.calculateDaysUntilNextPopup(lastShownEpochDay));
    }

    @Test
    public void notificationIsShownWhenTheLastOneIsAYearOld() {
        donationPreferences.setLastShownEpochDay((int) LocalDate.now().minusDays(400).toEpochDay());

        interact(donationProvider::showIfNeeded);

        verify(dialogService).notify(any(Notifications.DonationNotification.class));
        assertEquals((int) LocalDate.now().toEpochDay(), donationPreferences.getLastShownEpochDay());
    }

    @Test
    public void noNotificationIsShownWhenTheUserOptedOut() {
        donationPreferences.setNeverShowAgain(true);
        donationPreferences.setLastShownEpochDay((int) LocalDate.now().minusDays(400).toEpochDay());

        interact(donationProvider::showIfNeeded);

        verify(dialogService, never()).notify(any(Notifications.DonationNotification.class));
    }

    @Test
    public void neverShowAgainActionOptsTheUserOut() {
        donationPreferences.setLastShownEpochDay((int) LocalDate.now().minusDays(400).toEpochDay());
        interact(donationProvider::showIfNeeded);

        assertFalse(donationPreferences.isNeverShowAgain());
        assertEquals(OnClickBehaviour.HIDE_AND_REMOVE, triggerAction(0));
        assertTrue(donationPreferences.isNeverShowAgain());
    }

    /// GemsFX declares the actions as a raw list, hence the unchecked cast.
    @SuppressWarnings("unchecked")
    private OnClickBehaviour triggerAction(int index) {
        ArgumentCaptor<Notifications.DonationNotification> captor = ArgumentCaptor.forClass(Notifications.DonationNotification.class);
        verify(dialogService).notify(captor.capture());
        Notifications.DonationNotification notification = captor.getValue();
        NotificationAction<Object> action = notification.getActions().get(index);
        return action.getOnAction().call(notification);
    }
}
