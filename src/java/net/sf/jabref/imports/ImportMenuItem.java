package net.sf.jabref.imports;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import java.util.List;

public class ImportMenuItem extends JMenuItem implements ActionListener {
    
    ImportFormat importer;
    JabRefFrame frame;
    boolean openInNew;

    public ImportMenuItem(JabRefFrame frame, ImportFormat importer, boolean openInNew) {
	super(importer.getFormatName());
	this.frame = frame;
	this.importer = importer;
	this.openInNew = openInNew;
	addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        String tempFilename = frame.getNewFile();
	String encoding = Globals.prefs.get("defaultEncoding");
	if (tempFilename != null) {
	    try {
		List list = Globals.importFormatReader.importFromFile(importer, tempFilename);
		if (list != null)
		    frame.addBibEntries(list, tempFilename, openInNew);
	    } catch (IOException ex) {
		ex.printStackTrace();
	    }
	}
    }
}
