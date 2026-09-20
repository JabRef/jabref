package org.jabref.gui.welcome.components;

import java.time.LocalDate;

import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.Notifications;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.URLs;
import org.jabref.logic.l10n.Localization;

import com.dlsc.gemsfx.infocenter.Notification.OnClickBehaviour;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Asks the user for a donation once a year - shown in the notification center.
@NullMarked
public class DonationProvider {
    private static final int DONATION_INTERVAL_DAYS = 365;

    private final GuiPreferences preferences;
    private final DialogService dialogService;

    private @Nullable DelayedExecution scheduledShow;

    public DonationProvider(GuiPreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    public void showIfNeeded() {
        if (preferences.getDonationPreferences().isNeverShowAgain()) {
            return;
        }
        int lastShown = preferences.getDonationPreferences().getLastShownEpochDay();
        scheduleAfterDays(calculateDaysUntilNextPopup(lastShown));
    }

    public int calculateDaysUntilNextPopup(int lastShownEpochDay) {
        int today = (int) LocalDate.now().toEpochDay();
        if (lastShownEpochDay < 0) {
            return 7; // 7 days after first-launch, show the donation notification
        }
        return Math.max(0, DONATION_INTERVAL_DAYS - (today - lastShownEpochDay));
    }

    private void showNotification() {
        preferences.getDonationPreferences().setLastShownEpochDay((int) LocalDate.now().toEpochDay());

        Notifications.DonationNotification notification = new Notifications.DonationNotification(
                Localization.lang("Support JabRef"),
                Localization.lang("Help us improve JabRef by donating."));

        notification.getActions().add(new NotificationAction<>(Localization.lang("Never show again"), _ -> {
            preferences.getDonationPreferences().setNeverShowAgain(true);
            return OnClickBehaviour.HIDE_AND_REMOVE;
        }));

        notification.getActions().add(new NotificationAction<>(Localization.lang("Donate"), _ -> {
            new OpenBrowserAction(URLs.DONATE_URL, dialogService, preferences.getExternalApplicationsPreferences()).execute();
            return OnClickBehaviour.HIDE_AND_REMOVE;
        }));

        dialogService.notify(notification);
    }

    /// Schedules the next donation notification after a specified number of days to take care
    /// of situation where the user may leave the application open for an extended
    /// period.
    private void scheduleAfterDays(int days) {
        cancelScheduled();
        int delayDays = days;
        if (days <= 0) {
            showNotification();
            delayDays = DONATION_INTERVAL_DAYS;
        }
        scheduledShow = new DelayedExecution(Duration.hours(delayDays * 24), this::showIfNeeded);
        scheduledShow.start();
    }

    private void cancelScheduled() {
        if (scheduledShow != null) {
            scheduledShow.cancel();
            scheduledShow = null;
        }
    }

    public void cleanUp() {
        cancelScheduled();
    }
}
