package org.jabref.gui.citedrive;

import java.io.IOException;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citedrive.CiteDrivePush;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteDrivePushAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDrivePushAction.class);

    private final CiteDriveOAuthService citeDriveOAuthService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public CiteDrivePushAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;

        this.citeDriveOAuthService = new CiteDriveOAuthService(preferences.getExternalApplicationsPreferences(), preferences.getRemotePreferences(), preferences.getCiteDrivePreferences(), new OAuthSessionRegistry(), dialogService);
        // this.citeDriveOAuthService = new CiteDriveOAuthService(preferences.getExternalApplicationsPreferences(), preferences.getRemotePreferences(), preferences.getCiteDrivePreferences(), new OAuthSessionRegistry(), dialogService, URI.create("http://localhost:8080/default/authorize"), URI.create("http://localhost:8080/default/token"));

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        assert this.stateManager.getActiveDatabase().isPresent();

        BibDatabaseContext database = this.stateManager.getActiveDatabase().get();

        citeDriveOAuthService.getAccessToken().thenAccept(
                accessTokenOpt -> {
                    if (accessTokenOpt.isEmpty()) {
                        dialogService.notify(Localization.lang("CiteDrive push failed: could not obtain access token."));
                        return;
                    }

                    AccessToken accessToken = accessTokenOpt.get();
                    try {
                        CiteDrivePush.push(database, accessToken, preferences, dialogService);
                    } catch (IOException e) {
                        LOGGER.error("CiteDrive push failed", e);
                        dialogService.notify(Localization.lang("CiteDrive push failed: %0", e.getMessage()));
                    }
                });
    }
}
