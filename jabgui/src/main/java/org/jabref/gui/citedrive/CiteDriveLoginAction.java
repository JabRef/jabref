package org.jabref.gui.citedrive;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.citedrive.CiteDriveOAuthService;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteDriveLoginAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDriveLoginAction.class);
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CiteDriveOAuthService citeDriveOAuthService;

    public CiteDriveLoginAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.citeDriveOAuthService = createOAuthService(dialogService, preferences, oAuthSessionRegistry);

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    static CiteDriveOAuthService createOAuthService(DialogService dialogService, GuiPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry) {
        return new CiteDriveOAuthService(
                preferences.getRemotePreferences(),
                preferences.getCiteDrivePreferences(),
                oAuthSessionRegistry,
                uri -> UiTaskExecutor.runInJavaFXThread(() -> NativeDesktop.openBrowserShowPopup(uri.toASCIIString(), dialogService, preferences.getExternalApplicationsPreferences())));
    }

    /// User-facing reason of a failed login or push
    static String describeFailure(Throwable throwable) {
        Throwable cause = (throwable instanceof CompletionException && throwable.getCause() != null) ? throwable.getCause() : throwable;
        if (cause instanceof TimeoutException) {
            return Localization.lang("Login was not completed in time.");
        }
        return String.valueOf(cause.getMessage());
    }

    @Override
    public void execute() {
        assert this.stateManager.getActiveDatabase().isPresent();
        citeDriveOAuthService
                .authorizeInteractive()
                .exceptionally(throwable -> {
                    LOGGER.error("CiteDrive login failed", throwable);
                    dialogService.notify(Localization.lang("CiteDrive login failed: %0", describeFailure(throwable)));
                    return null;
                });
        // Token is ignored, because the refresh token is stored in the preferences
    }
}
