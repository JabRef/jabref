package net.sf.jabref.imports;

import net.sf.jabref.*;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.ImportInspectionDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

// TODO: could separate the "menu item" functionality from the importing functionality
public class ImportMenuItem extends JMenuItem implements ActionListener {

    JabRefFrame frame;
    boolean openInNew;
    MyWorker worker = null;
    ImportFormat importer;

    public ImportMenuItem(JabRefFrame frame, boolean openInNew) {
        this(frame, openInNew, null);
    }

    public ImportMenuItem(JabRefFrame frame, boolean openInNew, ImportFormat importer) {
        super(importer != null ? importer.getFormatName()
                : Globals.lang("Autodetect format"));
        this.importer = importer;
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
    
    /**
     * Automatically imports the files given as arguments
     * @param filenames List of files to import
     */
    public void automatedImport(String filenames[]) {
        // replace the work of the init step:
        MyWorker worker = new MyWorker();
        worker.fileOk = true;
        worker.filenames = filenames;
        
        worker.getWorker().run();
        worker.getCallBack().update();
    }
    

    class MyWorker extends AbstractWorker implements ImportInspectionDialog.CallBack {
        String[] filenames = null, formatName = null;
        ParserResult bibtexResult = null; // Contains the merged import results
        boolean fileOk = false;

        public void init() {
            filenames = Globals.getMultipleFiles(frame,
                    new File(Globals.prefs.get("workingDirectory")),
                    (importer != null ? importer.getExtensions() : null), true);

            /*if ((filenames != null) && !(new File(filename)).exists()) {
               JOptionPane.showMessageDialog(frame, Globals.lang("File not found") + ": '" + filename + "'",
                       Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
           } else*/
            if ((filenames != null) && (filenames.length > 0)) {
                frame.block();
                frame.output(Globals.lang("Starting import"));
                //frame.output(Globals.lang("Importing file") + ": '" + filename + "'");
                fileOk = true;
                
                Globals.prefs.put("workingDirectory", filenames[0]);
            }
        }

        public void run() {
            if (!fileOk)
                return;

            // We import all files and collect their results:
            List imports = new ArrayList();
            for (int i = 0; i < filenames.length; i++) {
                String filename = filenames[i];
                if (importer != null)
                    // Specific importer:
                    try {
                        imports.add(new Object[] { importer.getFormatName(),
                                Globals.importFormatReader.importFromFile(importer, filename)});
                    } catch (IOException e) {
                        // No entries found...
                    }
                else
                    // Unknown format:
                    imports.add(Globals.importFormatReader.importUnknownFormat(filename));
            }

            // Ok, done. Then try to gather in all we have found. Since we might have found
            // one or more bibtex results, it's best to gather them in a BibtexDatabase.
            bibtexResult = mergeImportResults(imports);
        }

        public void update() {
            if (!fileOk)
                return;

            // TODO: undo is not handled properly here, except for the entries added by
            //  the import inspection dialog.
            if (bibtexResult != null) {
                if (!openInNew) {
                    final BasePanel panel = (BasePanel) frame.getTabbedPane().getSelectedComponent();
                    BibtexDatabase toAddTo = panel.database();
                    // Use the import inspection dialog if it is enabled in preferences, and
                    // (there are more than one entry or the inspection dialog is also enabled
                    // for single entries):
                    if (Globals.prefs.getBoolean("useImportInspectionDialog") &&
                            (Globals.prefs.getBoolean("useImportInspectionDialogForSingle")
                                    || (bibtexResult.getDatabase().getEntryCount() > 1))) {
                        ImportInspectionDialog diag = new ImportInspectionDialog(frame, panel,
                                BibtexFields.DEFAULT_INSPECTION_FIELDS,
                                Globals.lang("Import"), openInNew);
                        diag.addEntries(bibtexResult.getDatabase().getEntries());
                        diag.addCallBack(this);
                        diag.entryListComplete();
                        Util.placeDialog(diag, frame);
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                        boolean generateKeys = Globals.prefs.getBoolean("generateKeysAfterInspection");
                        NamedCompound ce = new NamedCompound(Globals.lang("Import entries"));
                        for (Iterator i = bibtexResult.getDatabase().getEntries().iterator();
                             i.hasNext();) {
                            BibtexEntry entry = (BibtexEntry) i.next();
                            try {
                                // Check if the entry is a duplicate of an existing one:
                                boolean keepEntry = true;
                                BibtexEntry duplicate = Util.containsDuplicate(toAddTo, entry);
                                if (duplicate != null) {
                                    int answer = DuplicateResolverDialog.resolveDuplicateInImport
                                            (frame, duplicate, entry);
                                    // The upper entry is the
                                    if (answer == DuplicateResolverDialog.DO_NOT_IMPORT)
                                        keepEntry = false;
                                    if (answer == DuplicateResolverDialog.IMPORT_AND_DELETE_OLD) {
                                        // Remove the old one and import the new one.
                                        toAddTo.removeEntry(duplicate.getId());
                                        ce.addEdit(new UndoableRemoveEntry(toAddTo, duplicate, panel));
                                    }
                                }
                                // Add the entry, if we are supposed to:
                                if (keepEntry) {
                                    toAddTo.insertEntry(entry);
                                    // Generate key, if we are supposed to:
                                    if (generateKeys) {
                                        LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), toAddTo, entry);
                                        //System.out.println("gen:"+entry.getCiteKey());
                                    }
                                    
                                    ce.addEdit(new UndoableInsertEntry(toAddTo, entry, panel));
                                }
                            } catch (KeyCollisionException e) {
                                e.printStackTrace();
                            }
                        }
                        ce.end();
                        panel.undoManager.addEdit(ce);
                    }

                }

                else {
                    frame.addTab(bibtexResult.getDatabase(), bibtexResult.getFile(),
                            bibtexResult.getMetaData(), Globals.prefs.get("defaultEncoding"), true);
                    done(bibtexResult.getDatabase().getEntryCount());
                }


            } else {
                if (importer == null)
                    frame.output(Globals.lang("Could not find a suitable import format."));
                else
                    JOptionPane.showMessageDialog(frame, Globals.lang("No entries found. Please make sure you are "
								  +"using the correct import filter."), Globals.lang("Import failed"),
					      JOptionPane.ERROR_MESSAGE);
            }
            frame.unblock();
        }

        public void done(int entriesImported) {

            /*
            final BasePanel panel = (BasePanel) frame.getTabbedPane().getSelectedComponent();
            BibtexDatabase toAddTo = panel.database();

            // Add the strings, if any:
            for (Iterator i = bibtexResult.getDatabase().getStringKeySet().iterator(); i.hasNext();) {
                BibtexString s = bibtexResult.getDatabase().getString(i.next());
                try {
                    toAddTo.addString(s);
                } catch (KeyCollisionException e) {
                    e.printStackTrace();
                }

            }


            if ((panel != null) && (bibtexResult.getDatabase().getEntryCount() == 1)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        panel.highlightEntry((BibtexEntry)
                                bibtexResult.getDatabase().getEntries().
                                        iterator().next());
                    }
                });


            }
            */
            frame.output(Globals.lang("Imported entries") + ": " + entriesImported);

        }

        public void cancelled() {
            frame.output(Globals.lang("Import cancelled."));
        }


        // This method is called by the dialog when the user has cancelled or
        // signalled a stop. It is expected that any long-running fetch operations
        // will stop after this method is called.
        public void stopFetching() {
            // No process to stop.
        }
    }



    public static ParserResult mergeImportResults(List imports) {
        BibtexDatabase database = new BibtexDatabase();
        ParserResult directParserResult = null;
        boolean anythingUseful = false;

        for (Iterator iterator = imports.iterator(); iterator.hasNext();) {
            Object[] o = (Object[]) iterator.next();
            if (o[1] instanceof List) {
                List entries = (List) o[1];
                anythingUseful = anythingUseful | (entries.size() > 0);
                Util.setAutomaticFields(entries); // set timestamp and owner
                for (Iterator j = entries.iterator(); j.hasNext();) {
                    BibtexEntry entry = (BibtexEntry) j.next();
                    try {
                        entry.setId(Util.createNeutralId());
                        database.insertEntry(entry);
                    } catch (KeyCollisionException e) {
                        e.printStackTrace();
                    }
                }
            } else if (o[1] instanceof ParserResult) {
                // Bibtex result. We must merge it into our main base.
                ParserResult pr = (ParserResult) o[1];

                anythingUseful = anythingUseful
                        || ((pr.getDatabase().getEntryCount() > 0) || (pr.getDatabase().getStringCount() > 0));

                // Record the parserResult, as long as this is the first bibtex result:
                if (directParserResult == null) {
                    directParserResult = pr;
                }

                // Merge entries:
                for (Iterator j = pr.getDatabase().getEntries().iterator(); j.hasNext();) {
                    BibtexEntry entry = (BibtexEntry) j.next();
                    try {
                        database.insertEntry(entry);
                    } catch (KeyCollisionException e) {
                        e.printStackTrace(); // This should never happen
                    }
                }
                // Merge strings:
                for (Iterator j = pr.getDatabase().getStringKeySet().iterator(); j.hasNext();) {
                    BibtexString bs = (BibtexString)
                            (pr.getDatabase().getString(j.next()).clone());

                    try {
                        database.addString(bs);
                    } catch (KeyCollisionException e) {
                        // This means a duplicate string name exists, so it's not
                        // a very exceptional situation. We should maybe give a warning...?
                    }
                }

            }
        }

        if (!anythingUseful)
            return null;

        if ((imports.size() == 1) && (directParserResult != null)) {
            return directParserResult;
        } else {

            ParserResult pr = new ParserResult(database, new HashMap(), new HashMap());
            return pr;

        }
    }

}
