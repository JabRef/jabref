package net.sf.jabref.gui.util.bibsonomy;

import net.sf.jabref.Globals;
import net.sf.jabref.model.database.BibDatabaseContext;

import org.bibsonomy.rest.client.util.MultiDirectoryFileFactory;

public class JabRefFileFactory extends MultiDirectoryFileFactory {

	public JabRefFileFactory(BibDatabaseContext context) {
	    // pdf and ps directories are not supported any more
        // just
		super(context.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()).get(0),
                context.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()).get(0),
                context.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()).get(0));
	}

}
