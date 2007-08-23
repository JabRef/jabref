package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.ParamLayoutFormatter;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.GUIGlobals;

import java.io.File;

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
            else
                link = null;
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
        // Search in the standard file directory:
        /* TODO: oops, this part is not sufficient. We need access to the
         database's metadata in order to check if the database overrides
         the standard file directory */
        String dir = Globals.prefs.get(GUIGlobals.FILE_FIELD+"Directory");
		File f = Util.expandFilename(link, new String[] { dir, "." });

        /*
		 * Stumbled over this while investigating
		 *
		 * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
		 */
		if (f != null) {
			return f.getPath();//f.toURI().toString();
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
