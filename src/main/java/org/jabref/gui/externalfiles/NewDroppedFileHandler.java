package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.List;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class NewDroppedFileHandler {

    private final BibDatabaseContext bibDatabaseContext;
    private final ExternalFileTypes externalFileTypes;
    private final FileDirectoryPreferences fileDirectoryPreferences;

    public NewDroppedFileHandler(BibDatabaseContext bibDatabaseContext, ExternalFileTypes externalFileTypes, FileDirectoryPreferences fileDirectoryPreferences) {
        this.externalFileTypes = externalFileTypes;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
        this.bibDatabaseContext = bibDatabaseContext;
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

    public void addNewEntry(List<Path> files) {

    }

}
