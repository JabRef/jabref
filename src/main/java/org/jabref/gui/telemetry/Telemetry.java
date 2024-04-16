package org.jabref.gui.telemetry;

import java.util.Optional;
import java.util.TimerTask;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

public class Telemetry {
    private Telemetry() {
    }

    public static Optional<TelemetryClient> getTelemetryClient() {
        return Optional.empty();
    }

    private static void start(TelemetryPreferences preferences, BuildInfo buildInfo) {
    }

    public static void shutdown() {
        getTelemetryClient().ifPresent(client -> {
        });
    }

    public static void initTrackingNotification(DialogService dialogService, TelemetryPreferences preferences) {
        if (preferences.shouldAskToCollectTelemetry()) {
            JabRefExecutorService.INSTANCE.submit(new TimerTask() {
                @Override
                public void run() {
                    DefaultTaskExecutor.runInJavaFXThread(() -> showTrackingNotification(dialogService, preferences));
                }
            }, 60000); // run in one minute
        }
    }

    private static void showTrackingNotification(DialogService dialogService, TelemetryPreferences preferences) {
        boolean shouldCollect = preferences.shouldCollectTelemetry();

        if (!preferences.shouldCollectTelemetry()) {
            shouldCollect = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Telemetry: Help make JabRef better"),
                    Localization.lang("To improve the user experience, we would like to collect anonymous statistics on the features you use. We will only record what features you access and how often you do it. We will neither collect any personal data nor the content of bibliographic items. If you choose to allow data collection, you can later disable it via File -> Preferences -> General."),
                    Localization.lang("Share anonymous statistics"),
                    Localization.lang("Don't share"));
        }

        preferences.setCollectTelemetry(shouldCollect);
        preferences.setAskToCollectTelemetry(false);
    }
}
