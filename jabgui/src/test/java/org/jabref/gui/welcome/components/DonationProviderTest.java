package org.jabref.gui.welcome.components;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationProviderTest extends JavaFxTest {

    private static final int DISMISS_FOREVER = 0;
    private static final int DISMISS = 1;

    private final DialogService dialogService = mock(DialogService.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final DonationPreferences donationPreferences = new DonationPreferences(false, -1);
    private final DonationProvider donationProvider = new DonationProvider(preferences, dialogService);

    @BeforeEach
    void setUp() {
        when(preferences.getDonationPreferences()).thenReturn(donationPreferences);
    }

    @Test
    public void notificationIsDueWhenItWasScheduledForAPastDay() {
        LocalDate today = LocalDate.of(2026, 9, 9);
        assertEquals(0, donationProvider.calculateDaysUntilNextNotification((int) today.minusDays(30).toEpochDay(), today));
    }

    @Test
    public void notificationIsDueOnTheScheduledDay() {
        LocalDate today = LocalDate.of(2026, 9, 9);
        assertEquals(0, donationProvider.calculateDaysUntilNextNotification((int) today.toEpochDay(), today));
    }

    @Test
    public void daysUntilNextNotificationSpanTheWholeInterval() {
        LocalDate today = LocalDate.of(2026, 9, 9);
        LocalDate inSixMonths = today.plusMonths(6);
        assertEquals(ChronoUnit.DAYS.between(today, inSixMonths),
                donationProvider.calculateDaysUntilNextNotification((int) inSixMonths.toEpochDay(), today));
    }

    @Test
    public void firstLaunchSchedulesTheNotificationSixMonthsLater() {
        interact(donationProvider::showIfNeeded);

        assertEquals((int) LocalDate.now().plusMonths(6).toEpochDay(), donationPreferences.getNextNotificationEpochDay());
        verify(dialogService, never()).notify(any(Notifications.DonationNotification.class));
    }

    @Test
    public void dueNotificationIsShownAndScheduledSixMonthsLater() {
        donationPreferences.setNextNotificationEpochDay((int) LocalDate.now().minusDays(1).toEpochDay());

        interact(donationProvider::showIfNeeded);

        verify(dialogService).notify(any(Notifications.DonationNotification.class));
        assertEquals((int) LocalDate.now().plusMonths(6).toEpochDay(), donationPreferences.getNextNotificationEpochDay());
    }

    @Test
    public void dismissActionKeepsTheNotificationSixMonthsAway() {
        donationPreferences.setNextNotificationEpochDay((int) LocalDate.now().minusDays(1).toEpochDay());
        interact(donationProvider::showIfNeeded);

        assertEquals(OnClickBehaviour.HIDE_AND_REMOVE, triggerAction(DISMISS));
        assertEquals((int) LocalDate.now().plusMonths(6).toEpochDay(), donationPreferences.getNextNotificationEpochDay());
    }

    @Test
    public void dismissForeverActionStopsFutureNotifications() {
        donationPreferences.setNextNotificationEpochDay((int) LocalDate.now().minusDays(1).toEpochDay());
        interact(donationProvider::showIfNeeded);

        assertEquals(OnClickBehaviour.HIDE_AND_REMOVE, triggerAction(DISMISS_FOREVER));
        assertTrue(donationPreferences.isNeverShowAgain());
    }

    @Test
    public void noNotificationIsShownAfterDismissingForever() {
        donationPreferences.setNeverShowAgain(true);
        donationPreferences.setNextNotificationEpochDay((int) LocalDate.now().minusDays(1).toEpochDay());

        interact(donationProvider::showIfNeeded);

        verify(dialogService, never()).notify(any(Notifications.DonationNotification.class));
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
