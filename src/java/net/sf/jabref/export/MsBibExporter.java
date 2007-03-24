package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import java.util.Set;
import java.util.Iterator;

/**
 * Exporter for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed
 */
public class MsBibExporter extends ExportFormat {

    public MsBibExporter() {
        // We call the superclass contructor to define the format name and the
        // default file extension (is it .xml?):
        super(Globals.lang("MSBib"), "msbib", null, null, ".xml");
    }


    public void performExport(final BibtexDatabase database, final String file, final String encoding, Set entries) throws Exception {
        /*

            We need to override the performExport method, which is where the entries are
            exported to the file.

            database is the database to export.
            file is the name of the file to export to.
            encoding is the character encoding to use. If the MSBib format specifies a given
            encoding, I think the best choice is to ignore this argument.
            entries is the set of bibtex entry IDs to export. I have added some example code below
                to show how to get the entries.
         */

        System.out.println("Hello world MsBibExporter");
        
        //If entries==null, the entire database should be exported:
        if (entries == null)
            entries = database.getKeySet();

        // Accessing the entries:
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
            String id = (String)iterator.next();
            BibtexEntry entry = (BibtexEntry)database.getEntryById(id);

            // Do whatever with the entry here
        }

    }
}
