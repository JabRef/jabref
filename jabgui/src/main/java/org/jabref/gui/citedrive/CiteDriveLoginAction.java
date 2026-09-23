package org.jabref.gui.citedrive;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.citedrive.CiteDriveOAuthService;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.remote.RemotePreferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteDriveLoginAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDriveLoginAction.class);
    private final DialogService dialogService;
    private final CiteDriveOAuthService citeDriveOAuthService;

    public CiteDriveLoginAction(DialogService dialogService, GuiPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry) {
        this.dialogService = dialogService;
        this.citeDriveOAuthService = createOAuthService(dialogService, preferences, oAuthSessionRegistry);
    }

    static CiteDriveOAuthService createOAuthService(DialogService dialogService, GuiPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry) {
        return new CiteDriveOAuthService(
                preferences.getRemotePreferences(),
                preferences.getCiteDrivePreferences(),
                oAuthSessionRegistry,
                uri -> UiTaskExecutor.runInJavaFXThread(() -> NativeDesktop.openBrowserShowPopup(uri.toASCIIString(), dialogService, preferences.getExternalApplicationsPreferences())),
                () -> enableHttpServer(dialogService, preferences.getRemotePreferences()));
    }

    /// The browser returns to JabRef's HTTP server, which is off by default, so the user is asked to switch it on.
    ///
    /// @return whether the server is enabled now
    private static boolean enableHttpServer(DialogService dialogService, RemotePreferences remotePreferences) {
        boolean enable = Boolean.TRUE.equals(UiTaskExecutor.runInJavaFXThread(() -> dialogService.showConfirmationDialogAndWait(
                Localization.lang("Log in to CiteDrive"),
                Localization.lang("Logging in to CiteDrive needs JabRef's HTTP server, which is currently disabled."),
                Localization.lang("Enable HTTP server"))));
        if (enable) {
            remotePreferences.setEnableHttpServer(true);
        }
        return enable;
    }

    /// User-facing reason of a failed login or push
    static String describeFailure(Throwable throwable) {
        Throwable cause = (throwable instanceof CompletionException && throwable.getCause() != null) ? throwable.getCause() : throwable;
        if (cause instanceof TimeoutException) {
            return Localization.lang("Login was not completed in time.");
        }
        return String.valueOf(cause.getLocalizedMessage());
    }

    @Override
    public void execute() {
        // The token itself is not needed here: the refresh token is stored in the preferences
        citeDriveOAuthService
                .authorizeInteractive()
                .whenComplete((token, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("CiteDrive login failed", throwable);
                        dialogService.notify(Localization.lang("CiteDrive login failed: %0", describeFailure(throwable)));
                    } else if (token.isEmpty()) {
                        dialogService.notify(Localization.lang("CiteDrive login failed: could not obtain access token."));
                    }
                });
    }
}
