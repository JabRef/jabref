package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.logic.pdf.search.indexing.PdfIndexer;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalFilesEntryLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFilesEntryLinker.class);

    private final ExternalFileTypes externalFileTypes;
    private final FilePreferences filePreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final MoveFilesCleanup moveFilesCleanup;
    private final RenamePdfCleanup renameFilesCleanup;

    public ExternalFilesEntryLinker(ExternalFileTypes externalFileTypes, FilePreferences filePreferences, BibDatabaseContext bibDatabaseContext) {
        this.externalFileTypes = externalFileTypes;
        this.filePreferences = filePreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.moveFilesCleanup = new MoveFilesCleanup(bibDatabaseContext, filePreferences);
        this.renameFilesCleanup = new RenamePdfCleanup(false, bibDatabaseContext, filePreferences);
    }

    public Optional<Path> copyFileToFileDir(Path file) {
        Optional<Path> firstExistingFileDir = bibDatabaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingFileDir.isPresent()) {
            Path targetFile = firstExistingFileDir.get().resolve(file.getFileName());
            if (FileUtil.copyFile(file, targetFile, false)) {
                return Optional.of(targetFile);
            }
        }
        return Optional.empty();
    }

    public void renameLinkedFilesToPattern(BibEntry entry) {
        renameFilesCleanup.cleanup(entry);
    }

    public void moveLinkedFilesToFileDir(BibEntry entry) {
        moveFilesCleanup.cleanup(entry);
    }

    public void addFilesToEntry(BibEntry entry, List<Path> files) {
        for (Path file : files) {
            FileUtil.getFileExtension(file).ifPresent(ext -> {
                ExternalFileType type = externalFileTypes.getExternalFileTypeByExt(ext)
                                                         .orElse(new UnknownExternalFileType(ext));
                Path relativePath = FileUtil.relativize(file, bibDatabaseContext.getFileDirectories(filePreferences));
                LinkedFile linkedfile = new LinkedFile("", relativePath, type.getName());
                entry.addFile(linkedfile);
            });
        }
    }

    public void moveFilesToFileDirAndAddToEntry(BibEntry entry, List<Path> files, IndexingTaskManager indexingTaskManager) {
        try (AutoCloseable blocker = indexingTaskManager.blockNewTasks()) {
            addFilesToEntry(entry, files);
            moveLinkedFilesToFileDir(entry);
            renameLinkedFilesToPattern(entry);
        } catch (Exception e) {
            LOGGER.error("Could not block IndexingTaskManager", e);
        }

        try {
            indexingTaskManager.addToIndex(PdfIndexer.of(bibDatabaseContext, filePreferences), entry, bibDatabaseContext);
        } catch (IOException e) {
            LOGGER.error("Could not access Fulltext-Index", e);
        }
    }

    public void copyFilesToFileDirAndAddToEntry(BibEntry entry, List<Path> files, IndexingTaskManager indexingTaskManager) {
        try (AutoCloseable blocker = indexingTaskManager.blockNewTasks()) {
            for (Path file : files) {
                copyFileToFileDir(file)
                        .ifPresent(copiedFile -> addFilesToEntry(entry, Collections.singletonList(copiedFile)));
            }
            renameLinkedFilesToPattern(entry);
        } catch (Exception e) {
            LOGGER.error("Could not block IndexingTaskManager", e);
        }

        try {
            indexingTaskManager.addToIndex(PdfIndexer.of(bibDatabaseContext, filePreferences), entry, bibDatabaseContext);
        } catch (IOException e) {
            LOGGER.error("Could not access Fulltext-Index", e);
        }
    }
}
