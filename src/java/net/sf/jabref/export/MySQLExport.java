package net.sf.jabref.export;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Set;

import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.sql.SQLutil;

/**
 * MySQLExport contributed by Lee Patton.
 */
public class MySQLExport extends ExportFormat {

    public MySQLExport() {
        super(Globals.lang("MySQL database"), "mysql", null, null, ".sql");
    }

    /**
     * First method called when user starts the export.
     * 
     * @param database
     *            The bibtex database from which to export.
     * @param file
     *            The filename to which the export should be writtten.
     * @param encoding
     *            The encoding to use.
     * @param keySet
     *            The set of IDs of the entries to export.
     * @throws java.lang.Exception
     *             If something goes wrong, feel free to throw an exception. The
     *             error message is shown to the user.
     */
    public void performExport(final BibtexDatabase database,
        final MetaData metaData, final String file, final String encoding,
        Set<String> keySet) throws Exception {

        // open output file
        File outfile = new File(file);
        if (outfile.exists())
            outfile.delete();

        PrintStream fout = null;
        fout = new PrintStream(outfile);

        // get entries selected for export
        List<BibtexEntry> entries = FileActions.getSortedEntries(database,
            keySet, false);

        // create MySQL tables 
        SQLutil.dmlCreateTables(SQLutil.DBTYPE.MYSQL, fout);

        // populate entry_type table
        SQLutil.dmlPopTab_ET(fout);

        // populate entries table
        SQLutil.dmlPopTab_FD(entries, fout);

		GroupTreeNode gtn = metaData.getGroups();

		// populate groups table
        SQLutil.dmlPopTab_GP(gtn,fout);
        
		// populate entry_group table
        SQLutil.dmlPopTab_EG(gtn,fout);

        fout.close();

		return;
    }

}