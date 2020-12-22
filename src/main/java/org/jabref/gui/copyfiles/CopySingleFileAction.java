package org.jabref.gui.copyfiles;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;

import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.PreferencesService;

public class CopySingleFileAction extends SimpleCommand {

    private final LinkedFile linkedFile;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;

    private final BiFunction<Path, Path, Path> resolvePathFilename = (path, file) -> path.resolve(file.getFileName());

    public CopySingleFileAction(LinkedFile linkedFile, DialogService dialogService, BibDatabaseContext databaseContext, PreferencesService preferencesService) {
        this.linkedFile = linkedFile;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;

        this.executable.bind(Bindings.createBooleanBinding(
                () -> !linkedFile.isOnlineLink()
                        && linkedFile.findIn(databaseContext, preferencesService.getFilePreferences()).isPresent(),
                linkedFile.linkProperty()));
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferencesService.getWorkingDir())
                .build();
        Optional<Path> exportPath = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        exportPath.ifPresent(this::copyFileToDestination);
    }

    private void copyFileToDestination(Path exportPath) {
        Optional<Path> fileToExport = linkedFile.findIn(databaseContext, preferencesService.getFilePreferences());
        Optional<Path> newPath = OptionalUtil.combine(Optional.of(exportPath), fileToExport, resolvePathFilename);

        if (newPath.isPresent()) {
            Path newFile = newPath.get();
            boolean success = fileToExport.isPresent() && FileUtil.copyFile(fileToExport.get(), newFile, false);
            if (success) {
                dialogService.showInformationDialogAndWait(Localization.lang("Copy linked file"), Localization.lang("Successfully copied file to %0.", newPath.map(Path::getParent).map(Path::toString).orElse("")));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Copy linked file"), Localization.lang("Could not copy file to %0, maybe the file is already existing?", newPath.map(Path::getParent).map(Path::toString).orElse("")));
            }
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not resolve the file %0", fileToExport.map(Path::getParent).map(Path::toString).orElse("")));
        }
    }
}
