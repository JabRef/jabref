package net.sf.jabref.logic.layout.format;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

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

        List<ParsedFileField> fileList = FileField.parse(field);

        String link = null;
        if (fileType == null) {
            // No file type specified. Simply take the first link.
            if (!(fileList.isEmpty())) {
                link = fileList.get(0).getLink();
            }
        }
        else {
            // A file type is specified:
            for (ParsedFileField flEntry : fileList) {
                if (flEntry.getFileType().equalsIgnoreCase(fileType)) {
                    link = flEntry.getLink();
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

        Optional<File> f = FileUtil.expandFilename(link, dirs);

        /*
         * Stumbled over this while investigating
         *
         * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
         */
        if (f.isPresent()) {
            try {
                return f.get().getCanonicalPath();//f.toURI().toString();
            } catch (IOException e) {
                LOGGER.warn("Problem getting path", e);
                return f.get().getPath();
            }
        } else {
            return link;
        }

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
