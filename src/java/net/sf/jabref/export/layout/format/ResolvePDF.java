package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.*;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;

public class ResolvePDF implements LayoutFormatter {

    public String format(String field) {
	String dir = Globals.prefs.get("pdfDirectory");
	//Util.pr(Util.expandFilename(field, dir).getPath());
	return "file://"+Util.expandFilename(field, dir).getPath();
  }
}
