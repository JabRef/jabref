package org.jabref.gui.citedrive;

import java.io.IOException;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.citedrive.CiteDriveOAuthService;
import org.jabref.logic.citedrive.CiteDrivePush;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteDrivePushAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDrivePushAction.class);

    private final CiteDriveOAuthService citeDriveOAuthService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public CiteDrivePushAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.citeDriveOAuthService = CiteDriveLoginAction.createOAuthService(dialogService, preferences, oAuthSessionRegistry);

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        assert this.stateManager.getActiveDatabase().isPresent();

        BibDatabaseContext database = this.stateManager.getActiveDatabase().get();
        // Snapshot on the FX thread; the push runs in the background
        List<BibEntry> entries = database.getEntries().stream()
                                         .filter(entry -> !entry.isEmpty())
                                         .map(BibEntry::new)
                                         .toList();

        citeDriveOAuthService
                .getAccessToken()
                // Async: the token may arrive on the HTTP server thread; the push itself is network I/O
                .thenAcceptAsync(accessTokenOpt -> {
                    if (accessTokenOpt.isEmpty()) {
                        dialogService.notify(Localization.lang("CiteDrive push failed: could not obtain access token."));
                        return;
                    }
                    try {
                        if (CiteDrivePush.push(database, entries, accessTokenOpt.get(), preferences, dialogService)) {
                            // [impl->req~citedrive.push.import-page~1]
                            UiTaskExecutor.runInJavaFXThread(() -> NativeDesktop.openBrowserShowPopup(preferences.getCiteDrivePreferences().getImportPage().toASCIIString(), dialogService, preferences.getExternalApplicationsPreferences()));
                        }
                    } catch (IOException e) {
                        LOGGER.error("CiteDrive push failed", e);
                        dialogService.notify(Localization.lang("CiteDrive push failed: %0", e.getMessage()));
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("CiteDrive push failed", throwable);
                    dialogService.notify(Localization.lang("CiteDrive push failed: %0", CiteDriveLoginAction.describeFailure(throwable)));
                    return null;
                });
    }
}
