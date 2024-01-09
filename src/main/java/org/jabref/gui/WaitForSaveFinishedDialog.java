package org.jabref.gui;

import java.util.List;

import javafx.concurrent.Task;

import org.jabref.logic.l10n.Localization;

/**
 * Dialog shown when closing of application needs to wait for a save operation to finish.
 */
public class WaitForSaveFinishedDialog {

    private final DialogService dialogService;

    public WaitForSaveFinishedDialog(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public void showAndWait(List<LibraryTab> LibraryTabs) {
        if (LibraryTabs.stream().anyMatch(LibraryTab::isSaving)) {
            Task<Void> waitForSaveFinished = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    while (LibraryTabs.stream().anyMatch(LibraryTab::isSaving)) {
                        if (isCancelled()) {
                            return null;
                        } else {
                            Thread.sleep(100);
                        }
                    }
                    return null;
                }
            };

            dialogService.showProgressDialog(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for save operation to finish") + "...",
                    waitForSaveFinished
            );
        }
    }
}
