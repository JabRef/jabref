package net.sf.jabref.export.layout.format;

import java.io.File;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Will expand the relative PDF path and return a URI for the given file (which
 * must exist).
 * 
 * TODO Search also relative to Bib-file.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 */
public class ResolvePDF implements LayoutFormatter {

	public String format(String field) {

        // Search in the standard PDF directory:
        /* TODO: oops, this part is not sufficient. We need access to the
          database's metadata in order to check if the database overrides
          the standard file directory */
        String dir = Globals.prefs.get("pdfDirectory");
		File f = Util.expandFilename(field, new String[] { dir, "." });
		
		/*
		 * Stumbled over this while investigating
		 * 
		 * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
		 */
		if (f != null) {
			return f.toURI().toString();
		} else {
			return field;
		}
	}
}
