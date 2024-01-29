package org.jabref.gui;

import java.util.List;

import javafx.concurrent.Task;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;

/**
 * Dialog shown when <em>closing</em> of application needs to wait for a save operation to finish.
 */
public class ProcessingLibraryDialog {

    private final DialogService dialogService;

    public ProcessingLibraryDialog(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public void showAndWait(List<LibraryTab> libraryTabs) {
        if (libraryTabs.stream().anyMatch(tab -> tab.isSaving())) {
            Task<Void> waitForSaveFinished = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    while (libraryTabs.stream().anyMatch(tab -> tab.isSaving())) {
                        if (isCancelled()) {
                            return null;
                        } else {
                            Thread.sleep(100);
                        }
                    }
                    return null;
                }
            };

            DefaultTaskExecutor.runInJavaFXThread(waitForSaveFinished);
            dialogService.showProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for save operation to finish..."),
                    waitForSaveFinished
            );
        }
    }
}
