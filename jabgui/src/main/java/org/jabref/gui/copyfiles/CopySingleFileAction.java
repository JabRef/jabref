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

public class CopySingleFileAction extends SimpleCommand {

    private final List<LinkedFile> linkedFiles;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    private final BiFunction<Path, Path, Path> resolvePathFilename = (dir, file) -> dir.resolve(file.getFileName());

    public CopySingleFileAction(LinkedFile linkedFile,
                                DialogService dialogService,
                                BibDatabaseContext databaseContext,
                                FilePreferences filePreferences) {
        this(List.of(linkedFile), dialogService, databaseContext, filePreferences);
    }

    public CopySingleFileAction(Collection<LinkedFile> linkedFiles,
                                DialogService dialogService,
                                BibDatabaseContext databaseContext,
                                FilePreferences filePreferences) {
        this.linkedFiles = new ArrayList<>(linkedFiles);
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;

        this.executable.bind(Bindings.createBooleanBinding(
                () -> this.linkedFiles.stream().anyMatch(this::isLocalExisting),
                dependencies(this.linkedFiles)));
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration cfg = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        Optional<Path> exportDir = dialogService.showDirectorySelectionDialog(cfg);
        if (exportDir.isEmpty()) {
            return;
        }

        int ok = 0;
        List<String> failed = new ArrayList<>();

        for (LinkedFile file : linkedFiles) {
            Optional<Path> srcOpt = lf.findIn(databaseContext, filePreferences);
            if (srcOpt.isEmpty()) {
                continue;
            }
            Path src = srcOpt.get();
            Path dst = resolvePathFilename.apply(exportDir.get(), src);

            boolean success = FileUtil.copyFile(src, dst, false);
            if (success) {
                copiedFiles++;
            } else {
                failed.add(src.getFileName().toString());
            }
        }

        String title = Localization.lang("Copy linked file");

        if (linkedFiles.size() == 1) {
            if (ok == 1) {
                dialogService.showInformationDialogAndWait(
                        title,
                        Localization.lang("Successfully copied file to %0.",
                                exportDir.map(Path::toString).orElse("")));
            } else {
                dialogService.showErrorDialogAndWait(
                        title,
                        Localization.lang("Could not copy file to %0, maybe the file is already existing?",
                                exportDir.map(Path::toString).orElse("")));
            }
        } else {
            if (ok > 0 && failed.isEmpty()) {
                dialogService.showInformationDialogAndWait(
                        title,
                        Localization.lang("Successfully copied %0 file(s) to %1.",
                                Integer.toString(ok),
                                exportDir.map(Path::toString).orElse("")));
            } else if (ok > 0) {
                dialogService.showInformationDialogAndWait(
                        title,
                        Localization.lang("Copied %0 file(s). Failed: %1",
                                Integer.toString(ok),
                                String.join(", ", failed)));
            } else {
                dialogService.showErrorDialogAndWait(
                        title,
                        Localization.lang("Could not copy selected file(s)."));
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
