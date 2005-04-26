package net.sf.jabref.imports;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.File;

import net.sf.jabref.*;
import net.sf.jabref.gui.ImportInspectionDialog;

import java.util.List;

public class ImportUnknownMenuItem extends JMenuItem implements ActionListener,
        ImportInspectionDialog.CallBack {
    
    JabRefFrame frame;
    boolean openInNew;
    MyWorker worker = null;

    public ImportUnknownMenuItem(JabRefFrame frame, boolean openInNew) {
	super(Globals.lang("Autodetect format"));
	this.frame = frame;
	this.openInNew = openInNew;
	addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
	worker = new MyWorker();
	worker.init();
	worker.getWorker().run();
	worker.getCallBack().update();
    }

    class MyWorker extends AbstractWorker {
	String filename = null, formatName = null;
	java.util.List entries = null;
	ParserResult bibtexResult = null; // Used for a parsed Bibtex database, if that is the
	// correct format. Must be handled differently, because it can contain metadata and strings.
	boolean fileOk = false;
	public void init() {
	    filename = frame.getNewFile();
	    if ((filename != null) && !(new File(filename)).exists()) {
		JOptionPane.showMessageDialog(frame, Globals.lang("File not found")+": '"+filename+"'",
					      Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
	    }
	    else if (filename != null) {
		frame.block();
		frame.output(Globals.lang("Importing file")+": '"+filename+"'");
		fileOk = true;
	    }
	    
	}
	public void run() {
	    if (!fileOk)
		return;
	    
	    //try {
		Object[] o = Globals.importFormatReader.importUnknownFormat(filename);
		formatName = (String)o[0];
		if (o[1] instanceof java.util.List)
		    entries = (java.util.List)o[1];
		else if (o[1] instanceof ParserResult)
		    bibtexResult = (ParserResult)o[1];
	    //} catch (IOException ex) {
		//ex.printStackTrace();
	    //}
	}
	public void update() {
	    if (!fileOk)
		return;
	    
	    if (entries != null) {
            BasePanel panel = null;
            if (!openInNew) {
                panel = (BasePanel)frame.getTabbedPane().getSelectedComponent();
            }
            String[] fields = new String[] {"author", "title", "year" };
            ImportInspectionDialog diag = new ImportInspectionDialog(frame, panel, fields,
                    Globals.lang("Import"), openInNew);
            diag.addEntries(entries);
            diag.addCallBack(ImportUnknownMenuItem.this);
            diag.entryListComplete();
            Util.placeDialog(diag, frame);
            diag.setVisible(true);

		    //frame.addBibEntries(entries, filename, openInNew);

	    }
	    else if (bibtexResult != null) {
		frame.addTab(bibtexResult.getDatabase(), bibtexResult.getFile(), 
			     bibtexResult.getMetaData(), true);
		frame.output(Globals.lang("Opened database") + " '" + filename +
			     "' " + Globals.lang("with") + " " +
			     bibtexResult.getDatabase() .getEntryCount() + " " +
			     Globals.lang("entries") + ".");

	    }
	    else
		frame.output(Globals.lang("Could not find a suitable import format."));
	    frame.unblock();
	}
    }

    public void done(int entriesImported) {
        if (worker.entries != null) {
            frame.output(Globals.lang("Imported entries")+": "+entriesImported
    			     +"  "+Globals.lang("Format used")+": "+worker.formatName);
        }
    }


    // This method is called by the dialog when the user has cancelled or
    // signalled a stop. It is expected that any long-running fetch operations
    // will stop after this method is called.
    public void stopFetching() {
        // No process to stop.
    }
}
