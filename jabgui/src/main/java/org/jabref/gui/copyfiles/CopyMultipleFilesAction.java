package org.jabref.gui.copyfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public class CopyMultipleFilesAction extends SimpleCommand {

    private final ObservableList<LinkedFileViewModel> selectedFiles;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public CopyMultipleFilesAction(ObservableList<LinkedFileViewModel> selectedFiles,
                                   DialogService dialogService,
                                   BibDatabaseContext databaseContext,
                                   FilePreferences filePreferences) {
        this.selectedFiles = Objects.requireNonNull(selectedFiles);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
        this.executable.bind(Bindings.createBooleanBinding(
                () -> !selectedFiles.isEmpty() && selectedFiles.stream().allMatch(vm ->
                        !vm.getFile().isOnlineLink()
                                && vm.getFile().findIn(databaseContext, filePreferences).isPresent()),
                selectedFiles
        ));
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration conf = new DirectoryDialogConfiguration.Builder().build();
        dialogService.showDirectorySelectionDialog(conf).ifPresent(this::copyAllTo);
    }

    private void copyAllTo(Path targetDir) {
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Cannot create directory '%0'", targetDir));
            return;
        }

        int copied = 0;

        for (LinkedFileViewModel vm : selectedFiles) {
            Optional<Path> srcOpt = vm.getFile().findIn(databaseContext, filePreferences);
            if (srcOpt.isEmpty()) {
                continue;
            }

            Path src = srcOpt.get();
            Path dst = targetDir.resolve(src.getFileName());

            if (Files.exists(dst) && Files.isDirectory(dst)) {
                IOException ex = new IOException("Destination is a directory: " + dst);
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Cannot copy '%0' to '%1'", src, dst), ex);
                continue;
            }

            try {
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException ex) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Cannot copy '%0' to '%1'", src, dst), ex);
            }
        }

        if (copied > 0) {
            dialogService.notify(Localization.lang("Copied %0 file(s) to %1", copied, targetDir));
        }
    }
}
