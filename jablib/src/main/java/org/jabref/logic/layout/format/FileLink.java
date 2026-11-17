package org.jabref.logic.layout.format;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.layout.ParamLayoutFormatter;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.Nullable;

/// Export formatter that handles the file link list of JabRef 2.3 and later, by
/// selecting the first file link, if any, specified by the field.
public class FileLink implements ParamLayoutFormatter {

    private final List<Path> fileDirectories;
    private final @Nullable Path mainFileDirectory;
    private String fileType;

    public FileLink(List<Path> fileDirectories, @Nullable Path mainFileDirectory) {
        this.fileDirectories = fileDirectories;
        this.mainFileDirectory = mainFileDirectory;
    }

    @Override
    public String format(String field) {
        if (field == null) {
            return "";
        }

        List<LinkedFile> fileList = FileFieldParser.parse(field);

        LinkedFile link = null;
        if (fileType == null) {
            // No file type specified. Simply take the first link.
            if (!fileList.isEmpty()) {
                link = fileList.getFirst();
            }
        } else {
            // A file type is specified:
            for (LinkedFile flEntry : fileList) {
                if (flEntry.getFileType().equalsIgnoreCase(fileType)) {
                    link = flEntry;
                    break;
                }
            }
        }

        if (link == null) {
            return "";
        }

        // The main file directory is unset by default
        List<Path> dirs = fileDirectories.isEmpty()
                          ? Optional.ofNullable(mainFileDirectory).map(List::of).orElseGet(List::of)
                          : fileDirectories;

        return link.findIn(dirs)
                   .map(path -> path.normalize().toString())
                   .orElse(link.getLink());
    }

    /// This method is called if the layout file specifies an argument for this
    /// formatter. We use it as an indicator of which file type we should look for.
    ///
    /// @param arg The file type.
    @Override
    public void setArgument(String arg) {
        fileType = arg;
    }
}
