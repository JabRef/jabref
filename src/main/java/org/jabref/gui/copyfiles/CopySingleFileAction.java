package org.jabref.gui.copyfiles;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.JabRefPreferences;

public class CopySingleFileAction {

    private final LinkedFile linkedFile;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final BiFunction<Path, Path, Path> resolvePathFilename = (path, file) -> {
        return path.resolve(file.getFileName());
    };

    public CopySingleFileAction(LinkedFile linkedFile, DialogService dialogService, BibDatabaseContext databaseContext) {
        this.linkedFile = linkedFile;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
    }

    public void copyFile() {
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Path.of(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        Optional<Path> exportPath = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        exportPath.ifPresent(this::copyFileToDestination);
    }

    private void copyFileToDestination(Path exportPath) {
        Optional<Path> fileToExport = linkedFile.findIn(databaseContext, Globals.prefs.getFilePreferences());
        Optional<Path> newPath = OptionalUtil.combine(Optional.of(exportPath), fileToExport, resolvePathFilename);

        if (newPath.isPresent()) {
            Path newFile = newPath.get();
            boolean success = FileUtil.copyFile(fileToExport.get(), newFile, false);
            if (success) {
                dialogService.showInformationDialogAndWait(Localization.lang("Copy linked file"), Localization.lang("Sucessfully copied file to %0", newPath.map(Path::getParent).map(Path::toString).orElse("")));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Copy linked file"), Localization.lang("Could not copy file to %0, maybe the file is already existing?", newPath.map(Path::getParent).map(Path::toString).orElse("")));
            }
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not resolve the file %0", fileToExport.map(Path::getParent).map(Path::toString).orElse("")));
        }
    }
}
