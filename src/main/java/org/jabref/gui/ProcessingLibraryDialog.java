package org.jabref.gui;

import java.util.List;

import javafx.concurrent.Task;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;

/**
 * Dialog shown when closing of application needs to wait for a save operation to finish.
 *
 * TODO: Architecture smell. Instead of busy waiting, we should support a callback mechanism.
 *       For loading, this could be adapting {@link org.jabref.gui.util.BackgroundTask#onSuccess} to accept multiple
 *       callbacks.
 */
public class ProcessingLibraryDialog {

    public enum Mode { LOAD, SAVE }

    private final DialogService dialogService;
    private final Mode mode;

    public ProcessingLibraryDialog(DialogService dialogService, Mode mode) {
        this.dialogService = dialogService;
        this.mode = mode;
    }

    public void showAndWait(List<LibraryTab> libraryTabs) {
        if (libraryTabs.stream().anyMatch(tab -> mode == Mode.LOAD ? tab.isLoading() : tab.isSaving())) {
            Task<Void> waitForSaveFinished = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    while (libraryTabs.stream().anyMatch(tab -> tab.isLoading() || tab.isSaving())) {
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
                    mode == Mode.LOAD
                            ? Localization.lang("Waiting for libraries being processed.") + "..."
                            : Localization.lang("Waiting for save operation to finish.") + "...",
                    waitForSaveFinished
            );
        }
    }
}
