package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.LuceneManager;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalFilesEntryLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFilesEntryLinker.class);

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final MoveFilesCleanup moveFilesCleanup;
    private final RenamePdfCleanup renameFilesCleanup;
    private final DialogService dialogService;

    public ExternalFilesEntryLinker(ExternalApplicationsPreferences externalApplicationsPreferences, FilePreferences filePreferences, BibDatabaseContext bibDatabaseContext, DialogService dialogService) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.filePreferences = filePreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.moveFilesCleanup = new MoveFilesCleanup(bibDatabaseContext, filePreferences);
        this.renameFilesCleanup = new RenamePdfCleanup(false, bibDatabaseContext, filePreferences);
        this.dialogService = dialogService;
    }

    public Optional<Path> copyFileToFileDir(Path file) {
        Optional<Path> firstExistingFileDir = bibDatabaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingFileDir.isPresent()) {
            Path targetFile = firstExistingFileDir.get().resolve(file.getFileName());
            try {
                // See discussion at https://stackoverflow.com/questions/29368308/java-nio-how-is-files-issamefile-different-from-path-equals for using Files.isSameFile instead of Path.equals()
                if (Files.isSameFile(file, targetFile)) {
                    // In case of source == target, we pretend, we have success
                    return Optional.of(targetFile);
                }
            } catch (IOException e) {
                LOGGER.error("Could not check pre-condition for copy file.", e);
                return Optional.empty();
            }
            if (FileUtil.copyFile(file, targetFile, false)) {
                return Optional.of(targetFile);
            }
        }
        return Optional.empty();
    }

    public void copyLinkedFilesToFileDir(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();
        for (LinkedFile file : files) {
            LinkedFileHandler fileHandler = new LinkedFileHandler(file, entry, bibDatabaseContext, filePreferences);
            try {
                fileHandler.copyToDefaultDirectory();
            } catch (IOException exception) {
                LOGGER.error("Error while moving file {}", file.getLink(), exception);
            }
        }
        entry.setFiles(files);
    }

    public void renameLinkedFilesToPattern(BibEntry entry) {
        renameFilesCleanup.cleanup(entry);
    }

    public void moveLinkedFilesToFileDir(BibEntry entry) {
        moveFilesCleanup.cleanup(entry);
    }

    public void addFilesToEntry(BibEntry entry, List<Path> files) {
        List<Path> validFiles = getValidFileNames(files);
        for (Path file : validFiles) {
            FileUtil.getFileExtension(file).ifPresent(ext -> {
                ExternalFileType type = ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences)
                                                         .orElse(new UnknownExternalFileType(ext));
                Path relativePath = FileUtil.relativize(file, bibDatabaseContext, filePreferences);
                LinkedFile linkedfile = new LinkedFile("", relativePath, type.getName());
                entry.addFile(linkedfile);
            });
        }
    }

    public void moveFilesToFileDirRenameAndAddToEntry(BibEntry entry, List<Path> files, LuceneManager luceneManager) {
        try (AutoCloseable blocker = luceneManager.blockLinkedFileIndexer()) {
            addFilesToEntry(entry, files);
            moveLinkedFilesToFileDir(entry);
            renameLinkedFilesToPattern(entry);
        } catch (Exception e) {
            LOGGER.error("Could not block LinkedFilesIndexer", e);
        }
        luceneManager.updateAfterDropFiles(entry);
    }

    public void copyFilesToFileDirAndAddToEntry(BibEntry entry, List<Path> files, LuceneManager luceneManager) {
        try (AutoCloseable blocker = luceneManager.blockLinkedFileIndexer()) {
            for (Path file : files) {
                copyFileToFileDir(file)
                        .ifPresent(copiedFile -> addFilesToEntry(entry, Collections.singletonList(copiedFile)));
            }
            renameLinkedFilesToPattern(entry);
        } catch (Exception e) {
            LOGGER.error("Could not block LinkedFilesIndexer", e);
        }
        luceneManager.updateAfterDropFiles(entry);
    }

    private List<Path> getValidFileNames(List<Path> filesToAdd) {
        List<Path> validFileNames = new ArrayList<>();

        for (Path fileToAdd : filesToAdd) {
            if (FileUtil.detectBadFileName(fileToAdd.toString())) {
                String newFilename = FileNameCleaner.cleanFileName(fileToAdd.getFileName().toString());

                boolean correctButtonPressed = dialogService.showConfirmationDialogAndWait(Localization.lang("File \"%0\" cannot be added!", fileToAdd.getFileName()),
                        Localization.lang("Illegal characters in the file name detected.\nFile will be renamed to \"%0\" and added.", newFilename),
                        Localization.lang("Rename and add"));

                if (correctButtonPressed) {
                    Path correctPath = fileToAdd.resolveSibling(newFilename);
                    try {
                        Files.move(fileToAdd, correctPath);
                        validFileNames.add(correctPath);
                    } catch (IOException ex) {
                        LOGGER.error("Error moving file", ex);
                        dialogService.showErrorDialogAndWait(ex);
                    }
                }
            } else {
                validFileNames.add(fileToAdd);
            }
        }
        return validFileNames;
    }
}
