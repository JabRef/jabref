package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

public abstract class Exporter {

    private final String id;
    private final String displayName;
    private final FileType fileType;

    public Exporter(String id, String displayName, FileType extension) {
        this.id = id;
        this.displayName = displayName;
        this.fileType = extension;
    }

    /**
     * Returns a one-word ID (used, for example, to identify the exporter in the console).
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the exporter (to display to the user).
     */
    public String getName() {
        return displayName;
    }

    /**
     * Returns the type of files this exporter creates.
     */
    public FileType getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Performs the export.
     *
     * @param databaseContext the database to export from
     * @param file            the file to write to
     * @param encoding        the encoding to use
     * @param entries         a list containing all entries that should be exported
     */
    public abstract void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception;

    /**
     * Exports to all files linked to a given entry
     * @param databaseContext   the database to export from
     * @param encoding          the encoding to use
     * @param filePreferences   the filePreferences to use for resolving paths
     * @param entryToWriteOn    the entry for which we want to write on all linked pdfs
     * @param entriesToWrite    the content that we want to export to the pdfs
     * @return whether any file was written on
     * @throws Exception if the writing fails
     */
    public boolean exportToAllFilesOfEntry(BibDatabaseContext databaseContext, Charset encoding, FilePreferences filePreferences, BibEntry entryToWriteOn, List<BibEntry> entriesToWrite) throws Exception {
        boolean writtenToAFile = false;

        for (LinkedFile file : entryToWriteOn.getFiles()) {
            if (file.getFileType().equals(fileType.getName())) {
                Optional<Path> filePath = file.findIn(databaseContext, filePreferences);
                if (filePath.isPresent()) {
                    export(databaseContext, filePath.get(), encoding, entriesToWrite);
                    writtenToAFile = true;
                }
            }
        }

        return writtenToAFile;
    }

    /**
     * Exports bib-entries a file is linked to
     * Behaviour in case the file is linked to different bib-entries depends on the implementation of {@link #export}.
     * If it overwrites any existing information, only the last found bib-entry will be exported (as the previous exports are overwritten).
     * If it extends existing information, all found bib-entries will be exported.
     * @param databaseContext   the database-context to export from
     * @param dataBase          the database to export from
     * @param encoding          the encoding to use
     * @param filePreferences   the filePreferences to use for resolving paths
     * @param filePath          the path to the file we want to write on
     * @return whether the file was written on at least once
     * @throws Exception if the writing fails
     */
    public boolean exportToFileByPath(BibDatabaseContext databaseContext, BibDatabase dataBase, Charset encoding, FilePreferences filePreferences, Path filePath) throws Exception {
        if (!Files.exists(filePath)) {
            return false;
        }
        boolean writtenABibEntry = false;
        for (BibEntry entry : dataBase.getEntries()) {
            for (LinkedFile linkedFile : entry.getFiles()) {
                if (linkedFile.getFileType().equals(fileType.getName())) {
                    Optional<Path> linkedFilePath = linkedFile.findIn(databaseContext.getFileDirectories(filePreferences));
                    if (!linkedFilePath.isEmpty() && Files.exists(linkedFilePath.get()) && Files.isSameFile(linkedFilePath.get(), filePath)) {
                        export(databaseContext, filePath, encoding, Arrays.asList((entry)));
                        writtenABibEntry = true;
                    }
                }
            }
        }
        return writtenABibEntry;
    }
}
