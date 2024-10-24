package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

/*
    Responsible for handling file movement across directories
 */
public class FileDirectoryHandler {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final DialogService dialogService;

    public FileDirectoryHandler(BibDatabaseContext databaseContext, FilePreferences filePreferences, DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
    }

    public record DirectoryInfo(
            String label,
            Path path,
            DirectoryType type) {
    }

    public enum DirectoryType {
        MAIN,
        GENERAL,
        USER_SPECIFIC
    }

    public Optional<DirectoryInfo> determineTargetDirectory(Path currentFilePath) {
        BibDatabaseContext.FileDirectoriesInfo directoriesInfo = databaseContext.getFileDirectoriesInfo(filePreferences);
        Optional<DirectoryType> currentDirectory = determineCurrentDirectory(currentFilePath);

        // Handle based on available directories
        // If file is not in any known directory, prefer library-specific, then user-specific, then main
        return currentDirectory.map(directoryType -> switch (directoryType) {
            case MAIN,
                 USER_SPECIFIC ->
                    directoriesInfo.librarySpecificDirectory()
                                   .map(path -> new DirectoryInfo("library-specific file directory", path, DirectoryType.GENERAL));
            case GENERAL ->
                    directoriesInfo.userFileDirectory()
                                   .map(path -> new DirectoryInfo("user-specific file directory", path, DirectoryType.USER_SPECIFIC))
                                   .or(() -> Optional.of(new DirectoryInfo("main file directory", directoriesInfo.mainFileDirectory(), DirectoryType.MAIN)));
        }).orElseGet(() -> directoriesInfo.librarySpecificDirectory()
                                          .map(path -> new DirectoryInfo("library-specific file directory", path, DirectoryType.GENERAL))
                                          .or(() -> directoriesInfo.userFileDirectory()
                                                                   .map(path -> new DirectoryInfo("user-specific file directory", path, DirectoryType.USER_SPECIFIC)))
                                          .or(() -> Optional.of(new DirectoryInfo("main file directory", directoriesInfo.mainFileDirectory(), DirectoryType.MAIN))));

        // If file is in a known directory, determine where to move it
    }

    private Optional<DirectoryType> determineCurrentDirectory(Path filePath) {
        BibDatabaseContext.FileDirectoriesInfo directoriesInfo = databaseContext.getFileDirectoriesInfo(filePreferences);

        // Check main file directory
        if (filePath.startsWith(directoriesInfo.mainFileDirectory())) {
            return Optional.of(DirectoryType.MAIN);
        }

        // Check library-specific directory
        if (directoriesInfo.librarySpecificDirectory()
                           .map(filePath::startsWith)
                           .orElse(false)) {
            return Optional.of(DirectoryType.GENERAL);
        }

        // Check user-specific directory
        if (directoriesInfo.userFileDirectory()
                           .map(filePath::startsWith)
                           .orElse(false)) {
            return Optional.of(DirectoryType.USER_SPECIFIC);
        }

        return Optional.empty();
    }
}
