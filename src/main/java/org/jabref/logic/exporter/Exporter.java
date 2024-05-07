package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.FileType;
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
     * @param entries         a list containing all entries that should be exported
     */
    public abstract void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception;

    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries, List<Path> fileDirForDatabase, JournalAbbreviationRepository abbreviationRepository) throws Exception {
        export(databaseContext, file, entries);
    }

    /**
     * Exports to all files linked to a given entry
     *
     * @param databaseContext        the database to export from
     * @param filePreferences        the filePreferences to use for resolving paths
     * @param entryToWriteOn         the entry for which we want to write on all linked pdfs
     * @param entriesToWrite         the content that we want to export to the pdfs
     * @param abbreviationRepository the opened repository of journal abbreviations
     * @return whether any file was written on
     * @throws Exception if the writing fails
     */
    public boolean exportToAllFilesOfEntry(BibDatabaseContext databaseContext,
                                           FilePreferences filePreferences,
                                           BibEntry entryToWriteOn,
                                           List<BibEntry> entriesToWrite,
                                           JournalAbbreviationRepository abbreviationRepository) throws Exception {
        boolean writtenToAFile = false;

        for (LinkedFile file : entryToWriteOn.getFiles()) {
            if (file.getFileType().equals(fileType.getName())) {
                Optional<Path> filePath = file.findIn(databaseContext, filePreferences);
                if (filePath.isPresent()) {
                    export(databaseContext, filePath.get(), entriesToWrite, Collections.emptyList(), abbreviationRepository);
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
     *
     * @param databaseContext        the database-context to export from
     * @param filePreferences        the filePreferences to use for resolving paths
     * @param filePath               the path to the file we want to write on
     * @param abbreviationRepository the opened repository of journal abbreviations
     * @return whether the file was written on at least once
     * @throws Exception if the writing fails
     */
    public boolean exportToFileByPath(BibDatabaseContext databaseContext,
                                      FilePreferences filePreferences,
                                      Path filePath,
                                      JournalAbbreviationRepository abbreviationRepository) throws Exception {
        if (!Files.exists(filePath)) {
            return false;
        }
        boolean writtenABibEntry = false;
        for (BibEntry entry : databaseContext.getEntries()) {
            for (LinkedFile linkedFile : entry.getFiles()) {
                if (linkedFile.getFileType().equals(fileType.getName())) {
                    Optional<Path> linkedFilePath = linkedFile.findIn(databaseContext.getFileDirectories(filePreferences));
                    if (linkedFilePath.isPresent() && Files.exists(linkedFilePath.get()) && Files.isSameFile(linkedFilePath.get(), filePath)) {
                        export(databaseContext, filePath, List.of(entry), Collections.emptyList(), abbreviationRepository);
                        writtenABibEntry = true;
                    }
                }
            }
        }
        return writtenABibEntry;
    }
}
