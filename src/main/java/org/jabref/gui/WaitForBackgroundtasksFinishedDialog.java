package org.jabref.gui;

import javafx.concurrent.Task;
import org.jabref.logic.l10n.Localization;

import java.util.List;

/**
 * Dialog shown when closing of application needs to wait for some background tasks.
 */
public class WaitForBackgroundtasksFinishedDialog {

    private final DialogService dialogService;

    public WaitForBackgroundtasksFinishedDialog(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public void showAndWait(StateManager stateManager) {
        if (stateManager.anyTaskRunningBinding.getValue()) {
            Task<Void> waitForBackgroundtasksFinished = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    System.out.println("THREAD STARTED");
                    while (stateManager.anyTaskRunningBinding.getValue()) {
                        System.out.println("updated value to " + stateManager.tasksProgressBinding.getValue());
                        updateProgress(stateManager.tasksProgressBinding.getValue(), 1);
                        if (isCancelled()) {
                            return null;
                        } else {
                            Thread.sleep(100);
                        }
                    }
                    return null;
                }
            };

            Thread th = new Thread(waitForBackgroundtasksFinished);
            th.setDaemon(true);
            th.start();

            dialogService.showProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish") + "...",
                    waitForBackgroundtasksFinished
            );
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
