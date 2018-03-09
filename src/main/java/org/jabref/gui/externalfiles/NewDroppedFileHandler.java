package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDroppedFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewDroppedFileHandler.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final ExternalFileTypes externalFileTypes;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final MoveFilesCleanup moveFilesCleanup;

    public NewDroppedFileHandler(BibDatabaseContext bibDatabaseContext, ExternalFileTypes externalFileTypes, FileDirectoryPreferences fileDirectoryPreferences, String fileDirPattern) {
        this.externalFileTypes = externalFileTypes;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.moveFilesCleanup = new MoveFilesCleanup(bibDatabaseContext, fileDirPattern, fileDirectoryPreferences);
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

    public void addNewEntryFromXMPorPDFContent(List<Path> files) {

        for (Path file : files) {

            if (FileUtil.getFileExtension(file).filter(ext -> ext.equals("pdf")).isPresent()) {

                List<BibEntry> xmpEntriesInFile;
                try {
                    xmpEntriesInFile = XmpUtilReader.readXmp(file, Globals.prefs.getXMPPreferences());
                } catch (IOException e) {
                    LOGGER.warn("Problem reading XMP", e);
                    return;
                }

                if ((xmpEntriesInFile == null) || xmpEntriesInFile.isEmpty()) {
                    return;
                }
            }
        }
    }

    public void addToEntryAndMoveToFileDir(BibEntry entry, List<Path> files) {
        addFilesToEntry(entry, files);
        moveFilesCleanup.cleanup(entry);

    }

}
