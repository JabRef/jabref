package net.sf.jabref.export.layout.format;

import java.io.File;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.export.layout.ParamLayoutFormatter;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import java.io.IOException;

/**
 * Export formatter that handles the file link list of JabRef 2.3 and later, by
 * selecting the first file link, if any, specified by the field.
 */
public class FileLink implements ParamLayoutFormatter {

    String fileType = null;

    public String format(String field) {
        FileListTableModel tableModel = new FileListTableModel();
        if (field == null)
            return "";

        tableModel.setContent(field);
        String link = null;
        if (fileType == null) {
            // No file type specified. Simply take the first link.
            if (tableModel.getRowCount() > 0)
                link = tableModel.getEntry(0).getLink();
        }
        else {
            // A file type is specified:
            for (int i=0; i< tableModel.getRowCount(); i++) {
                FileListEntry flEntry = tableModel.getEntry(i);
                if (flEntry.getType().getName().toLowerCase().equals(fileType)) {
                    link = flEntry.getLink();
                    break;
                }
            }
        }
        
        if (link == null)
            return "";


        String dir;
        // We need to resolve the file directory from the database's metadata,
        // but that is not available from a formatter. Therefore, as an
        // ugly hack, the export routine has set a global variable before
        // starting the export, which contains the database's file directory:
        if (Globals.prefs.fileDirForDatabase != null)
            dir = Globals.prefs.fileDirForDatabase;
        else
            dir = Globals.prefs.get(GUIGlobals.FILE_FIELD + "Directory");
        
		File f = Util.expandFilename(link, new String[] { dir });

        /*
		 * Stumbled over this while investigating
		 *
		 * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
		 */
		if (f != null) {
            try {
                return f.getCanonicalPath();//f.toURI().toString();
            } catch (IOException e) {
                e.printStackTrace();
                return f.getPath();
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
    public void setArgument(String arg) {
        fileType = arg;
    }
}
