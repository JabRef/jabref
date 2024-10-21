package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
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

    public ExternalFilesEntryLinker(ExternalApplicationsPreferences externalApplicationsPreferences, FilePreferences filePreferences, BibDatabaseContext bibDatabaseContext) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.filePreferences = filePreferences;
        this.bibDatabaseContext = bibDatabaseContext;
    }

    public void linkFilesToEntry(BibEntry entry, List<Path> files) {
        List<LinkedFile> linkedFiles = files.stream().map(file -> {
            String typeName = FileUtil.getFileExtension(file)
                                      .map(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences).orElse(new UnknownExternalFileType(ext)).getName())
                                      .orElse("");
            Path relativePath = FileUtil.relativize(file, bibDatabaseContext, filePreferences);
            return new LinkedFile("", relativePath, typeName);
        }).toList();
        entry.addFiles(linkedFiles);
    }

    /**
     * <ul>
     *     <li>Move files to file directory</li>
     *     <li>Use configured file directory pattern</li>
     *     <li>Rename file to configured pattern (and skip renaming if file already exists)</li>
     *     <li>Avoid overwriting files - by adding " {number}" after the file name</li>
     * </ul>
     */
    public void coveOrMoveFilesSteps(BibEntry entry, List<Path> files, boolean shouldMove) {
        List<LinkedFile> linkedFiles = new ArrayList<>(files.size());
        // "old school" loop to enable logging properly
        for (Path file : files) {
            String typeName = FileUtil.getFileExtension(file)
                                      .map(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences).orElse(new UnknownExternalFileType(ext)).getName())
                                      .orElse("");
            LinkedFile linkedFile = new LinkedFile("", file, typeName);
            LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, bibDatabaseContext, filePreferences);
            try {
                linkedFileHandler.copyOrMoveToDefaultDirectory(shouldMove, false);
            } catch (IOException exception) {
                LOGGER.error("Error while copying/moving file {}", file, exception);
            }
            linkedFiles.add(linkedFile);
        }
        entry.addFiles(linkedFiles);
    }
}
