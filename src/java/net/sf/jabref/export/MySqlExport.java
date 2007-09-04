package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.MetaData;

import java.util.Set;
import java.util.List;
import java.io.File;

/**
 * Skeleton implementation of MySqlExporter.
 */
public class MySqlExport extends ExportFormat {

    public MySqlExport() {
        // You need to fill in the correct extension, and edit the name if necessary.
        // The second argument is the command-line name of the export format:
        super(Globals.lang("MySQL database"), "mysql", null, null, ".extension");
    }


    public void performExport(final BibtexDatabase database, final MetaData metaData,
                              final String file, final String encoding,
                              Set keySet) throws Exception {
        // This method gets called when the user starts the export.

        System.out.println("Hello world from MySqlExport");

        File outFile = new File(file);
        
        
        // One of the things you may want is to get a sorted list of which
        // bibtex entries need to be exported. This is done as follows:
        List<BibtexEntry> sorted = FileActions.getSortedEntries(database, keySet, false);
        // The method called above takes care of selecting all entries, or only those
        // that you should include in the export. So, once you get around to writing
        // entries, you can simply iterate over the list:
        for (BibtexEntry entry : sorted) {
            // Do something with the entry:
            String title = (String)entry.getField("title");
            String bibtexKey = entry.getCiteKey();
        }

    }
}
