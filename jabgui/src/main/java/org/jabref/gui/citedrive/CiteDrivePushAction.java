package org.jabref.gui.citedrive;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citedrive.OAuthSessionRegistry;

public class CiteDrivePushAction extends SimpleCommand {
    private final CiteDriveOAuthService citeDriveOAuthService;

    public CiteDrivePushAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        citeDriveOAuthService = new CiteDriveOAuthService(preferences.getExternalApplicationsPreferences(), preferences.getRemotePreferences(), preferences.getCiteDrivePreferences(), new OAuthSessionRegistry(), dialogService);

        this.executableProperty(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        citeDriveOAuthService.getAccessToken()
    }
}
