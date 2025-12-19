package org.jabref.gui.citedrive;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteDriveLoginAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDriveLoginAction.class);
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final CiteDriveOAuthService citeDriveOAuthService;

    public CiteDriveLoginAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.citeDriveOAuthService = new CiteDriveOAuthService(preferences.getExternalApplicationsPreferences(), preferences.getRemotePreferences(), preferences.getCiteDrivePreferences(), new OAuthSessionRegistry(), dialogService);

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        assert this.stateManager.getActiveDatabase().isPresent();
        citeDriveOAuthService
                .authorizeInteractive()
                .exceptionally(throwable -> {
                    LOGGER.error("CiteDrive login failed", throwable);
                    dialogService.notify(Localization.lang("CiteDrive login failed: %0", throwable.getMessage()));
                    return null;
                });
        // Token is ignored, because the refresh token is stored in the preferences
    }
}
