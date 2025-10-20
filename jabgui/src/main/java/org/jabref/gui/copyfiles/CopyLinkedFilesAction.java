package org.jabref.gui.copyfiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

public class CopyLinkedFilesAction extends SimpleCommand {

    private final List<LinkedFile> linkedFiles;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    private final BiFunction<Path, Path, Path> resolvePathFilename = (dir, file) -> dir.resolve(file.getFileName());

    public CopyLinkedFilesAction(LinkedFile linkedFile,
                                 DialogService dialogService,
                                 BibDatabaseContext databaseContext,
                                 FilePreferences filePreferences) {
        this(List.of(linkedFile), dialogService, databaseContext, filePreferences);
    }

    public CopyLinkedFilesAction(Collection<LinkedFile> linkedFiles,
                                 DialogService dialogService,
                                 BibDatabaseContext databaseContext,
                                 FilePreferences filePreferences) {
        this.linkedFiles = new ArrayList<>(linkedFiles);
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;

        this.executable.bind(Bindings.createBooleanBinding(
                () -> this.linkedFiles.stream().allMatch(this::isLocalExisting),
                dependencies(this.linkedFiles)));
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        Optional<Path> exportDir = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        if (exportDir.isEmpty()) {
            return;
        }

        int copiedFiles = 0;
        int failedCount = 0;

        for (LinkedFile file : linkedFiles) {
            Optional<Path> srcOpt = file.findIn(databaseContext, filePreferences);
            if (srcOpt.isEmpty()) {
                failedCount++;
                continue;
            }

            Path src = srcOpt.get();
            Path dst = resolvePathFilename.apply(exportDir.get(), src);

            if (FileUtil.copyFile(src, dst, false)) {
                copiedFiles++;
            } else {
                failedCount++;
            }
        }

        String target = exportDir.map(Path::toString).orElse("");

        if (linkedFiles.size() == 1) {
            if (copiedFiles == 1) {
                dialogService.notify(Localization.lang("Successfully copied %0 file(s) to %1.", copiedFiles, target));
            } else {
                dialogService.notify(Localization.lang("Could not copy file to %0, maybe the file is already existing?", target));
            }
        } else {
            if (failedCount == 0) {
                dialogService.notify(Localization.lang("Successfully copied %0 file(s) to %1.", copiedFiles, target));
            } else {
                dialogService.notify(Localization.lang("Copied %0 file(s). Failed: %1", copiedFiles, failedCount));
            }
        }
    }

    private boolean isLocalExisting(LinkedFile lf) {
        return !lf.isOnlineLink() && lf.findIn(databaseContext, filePreferences).isPresent();
    }

    private static Observable[] dependencies(Collection<LinkedFile> files) {
        return files.stream().map(LinkedFile::linkProperty).toArray(Observable[]::new);
    }
}
