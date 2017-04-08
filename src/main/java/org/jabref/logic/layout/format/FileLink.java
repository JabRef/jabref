package org.jabref.logic.layout.format;

import java.util.List;

import org.jabref.logic.layout.ParamLayoutFormatter;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.ParsedFileField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Export formatter that handles the file link list of JabRef 2.3 and later, by
 * selecting the first file link, if any, specified by the field.
 */
public class FileLink implements ParamLayoutFormatter {

    private static final Log LOGGER = LogFactory.getLog(FileLink.class);

    private String fileType;
    private final FileLinkPreferences prefs;


    public FileLink(FileLinkPreferences fileLinkPreferences) {
        this.prefs = fileLinkPreferences;
    }

    @Override
    public String format(String field) {
        if (field == null) {
            return "";
        }

        List<ParsedFileField> fileList = FileFieldParser.parse(field);

        ParsedFileField link = null;
        if (fileType == null) {
            // No file type specified. Simply take the first link.
            if (!(fileList.isEmpty())) {
                link = fileList.get(0);
            }
        }
        else {
            // A file type is specified:
            for (ParsedFileField flEntry : fileList) {
                if (flEntry.getFileType().equalsIgnoreCase(fileType)) {
                    link = flEntry;
                    break;
                }
            }
        }

        if (link == null) {
            return "";
        }

        List<String> dirs;
        // We need to resolve the file directory from the database's metadata,
        // but that is not available from a formatter. Therefore, as an
        // ugly hack, the export routine has set a global variable before
        // starting the export, which contains the database's file directory:
        if (prefs.getFileDirForDatabase() == null) {
            dirs = prefs.getGeneratedDirForDatabase();
        } else {
            dirs = prefs.getFileDirForDatabase();
        }

        return link.findIn(dirs)
                .map(path -> path.normalize().toString())
                .orElse(link.getLink());

    }

    /**
     * This method is called if the layout file specifies an argument for this
     * formatter. We use it as an indicator of which file type we should look for.
     * @param arg The file type.
     */
    @Override
    public void setArgument(String arg) {
        fileType = arg;
    }
}
