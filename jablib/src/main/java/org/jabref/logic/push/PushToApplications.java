package org.jabref.logic.push;

import java.util.Optional;

import org.jabref.logic.util.NotificationService;

public class PushToApplications {

    private PushToApplications() {
    }

    public static Optional<PushToApplication> getApplicationById(String applicationId, NotificationService notificationService, PushToApplicationPreferences pushToApplicationPreferences) {
        return PushApplications.getApplicationById(applicationId)
                               .flatMap(application -> getApplication(application, notificationService, pushToApplicationPreferences));
    }

    /// @param application Used by the CLI to select the application to run.
    public static Optional<PushToApplication> getApplication(PushApplications application, NotificationService notificationService, PushToApplicationPreferences preferences) {
        return Optional.of(switch (application) {
            case EMACS ->
                    new PushToEmacs(notificationService, preferences);
            case LYX ->
                    new PushToLyx(notificationService, preferences);
            case SUBLIME_TEXT ->
                    new PushToSublimeText(notificationService, preferences);
            case TEXMAKER ->
                    new PushToTexmaker(notificationService, preferences);
            case TEXSTUDIO ->
                    new PushToTeXstudio(notificationService, preferences);
            case TEXWORKS ->
                    new PushToTeXworks(notificationService, preferences);
            case VIM ->
                    new PushToVim(notificationService, preferences);
            case WIN_EDT ->
                    new PushToWinEdt(notificationService, preferences);
            case TEXSHOP ->
                    new PushToTexShop(notificationService, preferences);
            case VSCODE ->
                    new PushToVScode(notificationService, preferences);
        });
    }
}
