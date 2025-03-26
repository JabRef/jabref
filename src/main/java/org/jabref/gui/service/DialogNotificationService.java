package org.jabref.gui.service;

import org.jabref.gui.DialogService;
import org.jabref.logic.service.NotificationService;

/**
 * GUI implementation of the NotificationService that delegates to the DialogService.
 * This class bridges the architectural gap between Logic and GUI layers by implementing
 * the Logic-layer interface while using GUI-layer components internally.
 */
public class DialogNotificationService implements NotificationService {

    private final DialogService dialogService;

    /**
     * Creates a new DialogNotificationService
     *
     * @param dialogService The dialog service to delegate notifications t
     */
    public DialogNotificationService(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @Override
    public void notify(String message) {
        dialogService.notify(message);
    }

    @Override
    public void showErrorDialog(String message) {
        dialogService.showErrorDialogAndWait(message);
    }
}
