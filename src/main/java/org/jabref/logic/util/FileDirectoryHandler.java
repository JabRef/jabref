package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

/*
    Responsible for handling file movement across directories
 */
public class FileDirectoryHandler {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public FileDirectoryHandler(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    public record DirectoryInfo(
            String label,
            Path path,
            DirectoryType type) {
    }

    public enum DirectoryType {
        MAIN,
        LIBRARY_SPECIFIC,
        USER_SPECIFIC
    }

    public Optional<DirectoryInfo> determineTargetDirectory(Path currentFilePath) {
        BibDatabaseContext.FileDirectoriesInfo directoriesInfo = databaseContext.getFileDirectoriesInfo(filePreferences);
        Optional<DirectoryType> currentDirectory = determineCurrentDirectory(currentFilePath);

        // Check if any directory exists
        boolean hasMain = directoriesInfo.mainFileDirectory() != null && !directoriesInfo.mainFileDirectory().toString().isEmpty();
        boolean hasLibrary = directoriesInfo.librarySpecificDirectory().isPresent();
        boolean hasUser = directoriesInfo.userFileDirectory().isPresent();

        // If no directory exists, return empty
        if (!hasMain && !hasLibrary && !hasUser) {
            return Optional.empty();
        }
        // Handle based on available directories
        return currentDirectory.map(directoryType -> {
            Optional<DirectoryInfo> result = switch (directoryType) {
                case MAIN -> {
                    // File is in main directory, prefer library specific, then user specific
                    if (hasLibrary) {
                        yield Optional.of(new DirectoryInfo("library-specific file directory",
                                directoriesInfo.librarySpecificDirectory().get(), DirectoryType.LIBRARY_SPECIFIC));
                    } else if (hasUser) {
                        yield Optional.of(new DirectoryInfo("user-specific file directory",
                                directoriesInfo.userFileDirectory().get(), DirectoryType.USER_SPECIFIC));
                    } else {
                        yield Optional.empty();
                    }
                }
                case LIBRARY_SPECIFIC -> {
                    // File is in library directory, prefer user specific, then main
                    if (hasUser) {
                        yield Optional.of(new DirectoryInfo("user-specific file directory",
                                directoriesInfo.userFileDirectory().get(), DirectoryType.USER_SPECIFIC));
                    } else if (hasMain) {
                        yield Optional.of(new DirectoryInfo("main file directory",
                                directoriesInfo.mainFileDirectory(), DirectoryType.MAIN));
                    } else {
                        yield Optional.empty();
                    }
                }
                case USER_SPECIFIC -> {
                    // File is in user directory, prefer library specific
                    if (hasLibrary) {
                        yield Optional.of(new DirectoryInfo("library-specific file directory",
                                directoriesInfo.librarySpecificDirectory().get(), DirectoryType.LIBRARY_SPECIFIC));
                    } else if (hasMain) {
                        yield Optional.of(new DirectoryInfo("main file directory",
                                directoriesInfo.mainFileDirectory(), DirectoryType.MAIN));
                    } else {
                        yield Optional.empty();
                    }
                }
            };
            return result;
        }).orElseGet(() -> {
            // File is outside all directories, follow priority: library > user > main
            if (hasLibrary) {
                return Optional.of(new DirectoryInfo("library-specific file directory",
                        directoriesInfo.librarySpecificDirectory().get(), DirectoryType.LIBRARY_SPECIFIC));
            } else if (hasUser) {
                return Optional.of(new DirectoryInfo("user-specific file directory",
                        directoriesInfo.userFileDirectory().get(), DirectoryType.USER_SPECIFIC));
            } else {
                return Optional.of(new DirectoryInfo("main file directory",
                        directoriesInfo.mainFileDirectory(), DirectoryType.MAIN));
            }
        });
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
            return Optional.of(DirectoryType.LIBRARY_SPECIFIC);
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
