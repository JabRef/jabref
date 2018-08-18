package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class ExternalFilesEntryLinker {

    private final ExternalFileTypes externalFileTypes;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final MoveFilesCleanup moveFilesCleanup;
    private final RenamePdfCleanup renameFilesCleanup;

    public ExternalFilesEntryLinker(ExternalFileTypes externalFileTypes, FileDirectoryPreferences fileDirectoryPreferences, String fileDirPattern, BibDatabaseContext bibDatabaseContext, String fileNamePattern) {
        this.externalFileTypes = externalFileTypes;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.moveFilesCleanup = new MoveFilesCleanup(bibDatabaseContext, fileDirPattern, fileDirectoryPreferences);
        this.renameFilesCleanup = new RenamePdfCleanup(false, bibDatabaseContext, fileNamePattern, fileDirectoryPreferences);
    }

    public Optional<Path> copyFileToFileDir(Path file) {
        Optional<Path> firstExistingFileDir = bibDatabaseContext.getFirstExistingFileDir(fileDirectoryPreferences);
        if (firstExistingFileDir.isPresent()) {
            Path targetFile = firstExistingFileDir.get().resolve(file.getFileName());
            if (FileUtil.copyFile(file, targetFile, false)) {
                return Optional.ofNullable(targetFile);
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

    public void addFileToEntry(BibEntry entry, Path file) {
        addFilesToEntry(entry, Arrays.asList(file));
    }

    public void addFilesToEntry(BibEntry entry, List<Path> files) {

        for (Path file : files) {
            FileUtil.getFileExtension(file).ifPresent(ext -> {

                ExternalFileType type = externalFileTypes.getExternalFileTypeByExt(ext)
                                                         .orElse(new UnknownExternalFileType(ext));
                Path relativePath = FileUtil.shortenFileName(file, bibDatabaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences));
                LinkedFile linkedfile = new LinkedFile("", relativePath.toString(), type.getName());
                entry.addFile(linkedfile);
            });

        }

    }
}
