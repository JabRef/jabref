package org.jabref.gui.fieldeditors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

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
        MAIN,          // Main file directory
        GENERAL,       // General (library) specific directory
        USER_SPECIFIC  // User specific directory
    }

    public Optional<DirectoryInfo> determineTargetDirectory(Path currentFilePath) {
        List<DirectoryInfo> availableDirectories = getAvailableDirectories();
        System.out.println(availableDirectories.size());
        if (availableDirectories.isEmpty()) {
            return Optional.empty();
        }

        Optional<DirectoryType> currentDirectory = determineCurrentDirectory(currentFilePath);
        System.out.println("Current Directory");
        System.out.println(currentDirectory);
        if (availableDirectories.size() == 1) {
            // If only one directory exists and file is outside, return that directory
            if (currentDirectory.isEmpty()) {
                return Optional.of(availableDirectories.getFirst());
            }
        } else if (availableDirectories.size() == 2) {
            return handleTwoDirectoriesCase(currentDirectory, availableDirectories);
        } else if (availableDirectories.size() == 3) {
            return handleThreeDirectoriesCase(currentDirectory, availableDirectories);
        }

        return Optional.empty();
    }

    private List<DirectoryInfo> getAvailableDirectories() {
        List<DirectoryInfo> directories = new ArrayList<>();
        MetaData metaData = databaseContext.getMetaData();

        // Main file directory
        String mainFilePath = String.valueOf(filePreferences.getMainFileDirectory());
        if (mainFilePath != null) { // Check if the path is not null
            directories.add(new DirectoryInfo(Localization.lang("main file directory"), Path.of(mainFilePath), DirectoryType.MAIN));
        }

        // General (library) specific directory from MetaData
        metaData.getDefaultFileDirectory().ifPresent(path ->
                directories.add(new DirectoryInfo(
                        Localization.lang("library-specific file directory"),
                        Path.of(path),
                        DirectoryType.GENERAL
                ))
        );

        // User-specific directory from MetaData
        metaData.getUserFileDirectory(filePreferences.getUserAndHost()).ifPresent(path ->
                directories.add(new DirectoryInfo(
                        Localization.lang("user-specific file directory"),
                        Path.of(path),
                        DirectoryType.USER_SPECIFIC
                ))
        );

        for (DirectoryInfo directory : directories) {
            System.out.println(directory);
        }

        return directories;
    }

    private Optional<DirectoryType> determineCurrentDirectory(Path filePath) {
        MetaData metaData = databaseContext.getMetaData();

        // Check main directory
        String mainFilePath = String.valueOf(filePreferences.getMainFileDirectory());
        if (mainFilePath != null && filePath.startsWith(mainFilePath)) {
            return Optional.of(DirectoryType.MAIN);
        }

        // Check general directory
        if (metaData.getDefaultFileDirectory()
                    .map(dir -> filePath.startsWith(Path.of(dir)))
                    .orElse(false)) {
            return Optional.of(DirectoryType.GENERAL);
        }

        // Check user specific directory
        if (metaData.getUserFileDirectory(filePreferences.getUserAndHost())
                    .map(dir -> filePath.startsWith(Path.of(dir)))
                    .orElse(false)) {
            return Optional.of(DirectoryType.USER_SPECIFIC);
        }

        return Optional.empty();
    }

    private Optional<DirectoryInfo> handleTwoDirectoriesCase(Optional<DirectoryType> currentDirectory, List<DirectoryInfo> availableDirectories) {
        String mainFilePath = String.valueOf(filePreferences.getMainFileDirectory());

        if (currentDirectory.isEmpty()) {
            // File outside both directories - prefer general, then user-specific, then main
            return availableDirectories.stream()
                                       .filter(dir -> dir.type == DirectoryType.GENERAL)
                                       .findFirst()
                                       .or(() -> availableDirectories.stream()
                                                                     .filter(dir -> dir.type == DirectoryType.USER_SPECIFIC)
                                                                     .findFirst())
                                       .or(() -> mainFilePath != null ? availableDirectories.stream()
                                                                                            .filter(dir -> dir.type == DirectoryType.MAIN && dir.path.startsWith(mainFilePath))
                                                                                            .findFirst() : Optional.empty());
        }

        DirectoryType current = currentDirectory.get();
        if (current == DirectoryType.MAIN) {
            // If file is in main directory, prefer general, then user-specific
            return availableDirectories.stream()
                                       .filter(dir -> dir.type == DirectoryType.GENERAL || dir.type == DirectoryType.USER_SPECIFIC)
                                       .findFirst();
        } else if (current == DirectoryType.GENERAL) {
            // If file is in general directory, prefer main (user-specific cannot exist in this case)
            return availableDirectories.stream()
                                       .filter(dir -> dir.type == DirectoryType.MAIN && dir.path.startsWith(mainFilePath))
                                       .findFirst();
        } else {
            // If file is in user-specific directory, prefer main (general cannot exist in this case)
            return availableDirectories.stream()
                                       .filter(dir -> dir.type == DirectoryType.MAIN && dir.path.startsWith(mainFilePath))
                                       .findFirst();
        }
    }

    private Optional<DirectoryInfo> handleThreeDirectoriesCase(Optional<DirectoryType> currentDirectory, List<DirectoryInfo> availableDirectories) {
        String mainFilePath = String.valueOf(filePreferences.getMainFileDirectory());

        if (currentDirectory.isEmpty()) {
            // File outside all directories - show general
            return availableDirectories.stream()
                                       .filter(dir -> dir.type == DirectoryType.GENERAL)
                                       .findFirst();
        }

        DirectoryType current = currentDirectory.get();
        return switch (current) {
            case MAIN, USER_SPECIFIC -> availableDirectories.stream()
                                                            .filter(dir -> dir.type == DirectoryType.GENERAL)
                                                            .findFirst();
            case GENERAL -> availableDirectories.stream()
                                                .filter(dir -> dir.type == DirectoryType.USER_SPECIFIC)
                                                .findFirst();
        };
    }
}
