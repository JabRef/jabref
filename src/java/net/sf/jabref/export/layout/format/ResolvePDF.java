package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.*;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;
import java.io.File;

public class ResolvePDF implements LayoutFormatter {

	public String format(String field) {
		String dir = Globals.prefs.get("pdfDirectory");
		File f = Util.expandFilename(field, new String[]{dir, "."});
		return (f != null ? "file://" + f.getPath() : field);
	}
}
