/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.bibtex.DuplicateCheck;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.util.Util;

/* 
 * TODO: could separate the "menu item" functionality from the importing functionality
 * 
 */
public class ImportMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6164143717906802116L;
    private final JabRefFrame frame;
    private final boolean openInNew;
    private final ImportFormat importer;
    private IOException importError;


    public ImportMenuItem(JabRefFrame frame, boolean openInNew) {
        this(frame, openInNew, null);
    }

    public ImportMenuItem(JabRefFrame frame, boolean openInNew, ImportFormat importer) {
        super(importer != null ? importer.getFormatName()
                : Localization.lang("Autodetect format"));
        this.importer = importer;
        this.frame = frame;
        this.openInNew = openInNew;
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MyWorker worker = new MyWorker();
        worker.init();
        worker.getWorker().run();
        worker.getCallBack().update();
    }

    /**
     * Automatically imports the files given as arguments
     * @param filenames List of files to import
     */
    public void automatedImport(String[] filenames) {
        // replace the work of the init step:
        MyWorker worker = new MyWorker();
        worker.fileOk = true;
        worker.filenames = filenames;

        worker.getWorker().run();
        worker.getCallBack().update();
    }


    class MyWorker extends AbstractWorker {

        String[] filenames;
        ParserResult bibtexResult; // Contains the merged import results
        boolean fileOk;


        @Override
        public void init() {
            importError = null;
            filenames = FileDialogs.getMultipleFiles(frame,
                    new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)),
                    importer != null ? importer.getExtensions() : null, true);

            if ((filenames != null) && (filenames.length > 0)) {
                frame.block();
                frame.output(Localization.lang("Starting import"));
                fileOk = true;

                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, filenames[0]);
            }
        }

        @Override
        public void run() {
            if (!fileOk) {
                return;
            }

            // We import all files and collect their results:
            List<ImportFormatReader.UnknownFormatImport> imports = new ArrayList<ImportFormatReader.UnknownFormatImport>();
            for (String filename : filenames) {
                try {
                    if (importer != null) {
                        // Specific importer:
                        ParserResult pr = new ParserResult(
                                Globals.importFormatReader.importFromFile(importer,
                                        filename, frame));

                        imports.add(new ImportFormatReader.UnknownFormatImport(importer
                                .getFormatName(), pr));
                    } else {
                        // Unknown format:
                        frame.output(Localization.lang("Importing in unknown format") + "...");
                        // This import method never throws an IOException:
                        imports.add(Globals.importFormatReader
                                .importUnknownFormat(filename));
                    }
                } catch (IOException e) {
                    // This indicates that a specific importer was specified, and that
                    // this importer has thrown an IOException. We store the exception,
                    // so a relevant error message can be displayed.
                    importError = e;
                }
            }

            // Ok, done. Then try to gather in all we have found. Since we might
            // have found
            // one or more bibtex results, it's best to gather them in a
            // BibtexDatabase.
            bibtexResult = mergeImportResults(imports);

            /* show parserwarnings, if any. */
            for (ImportFormatReader.UnknownFormatImport p : imports) {
                if (p != null) {
                    ParserResult pr = p.parserResult;
                    if (pr.hasWarnings()) {
                        if (Globals.prefs
                                .getBoolean(JabRefPreferences.DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP)
                                && pr.hasWarnings()) {
                            String[] wrns = pr.warnings();
                            StringBuilder wrn = new StringBuilder();
                            for (int j = 0; j < wrns.length; j++) {
                                wrn.append(j + 1).append(". ").append(wrns[j])
                                        .append("\n");
                            }
                            if (wrn.length() > 0) {
                                wrn.deleteCharAt(wrn.length() - 1);
                            }
                            JOptionPane.showMessageDialog(frame, wrn.toString(),
                                    Localization.lang("Warnings"),
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }
        }

        @Override
        public void update() {
            if (!fileOk) {
                return;
            }

            // TODO: undo is not handled properly here, except for the entries
            // added by
            //  the import inspection dialog.
            if (bibtexResult != null) {
                if (!openInNew) {
                    final BasePanel panel = (BasePanel) frame.getTabbedPane().getSelectedComponent();
                    BibtexDatabase toAddTo = panel.database();

                    // Use the import inspection dialog if it is enabled in preferences, and
                    // (there are more than one entry or the inspection dialog is also enabled
                    // for single entries):
                    if (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG) &&
                            (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG_FOR_SINGLE)
                            || (bibtexResult.getDatabase().getEntryCount() > 1))) {
                        ImportInspectionDialog diag = new ImportInspectionDialog(frame, panel,
                                BibtexFields.DEFAULT_INSPECTION_FIELDS,
                                Localization.lang("Import"), openInNew);
                        diag.addEntries(bibtexResult.getDatabase().getEntries());
                        diag.entryListComplete();
                        Util.placeDialog(diag, frame);
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                        Integer addCount = 0;
                        Integer replaceCount = 0;
                        Integer mergeCount = 0;
                        String preString = null;
                        boolean generateKeys = Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION);
                        NamedCompound ce = new NamedCompound(Localization.lang("Import entries"));

                        // Check if we should unmark entries before adding the new ones:
                        if (Globals.prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING)) {
                            for (BibtexEntry entry : toAddTo.getEntries()) {
                                EntryMarker.unmarkEntry(entry, true, toAddTo, ce);
                            }
                        }

                        mainLoop:
                        for (BibtexEntry entry : bibtexResult.getDatabase().getEntries()) {
                            try {
                                // Check if the entry is a duplicate of an existing one:
                                boolean keepEntry = true;
                                boolean remove = false;
                                BibtexEntry mergedEntry = null;
                                BibtexEntry duplicate = DuplicateCheck.containsDuplicate(toAddTo, entry);
                                if (duplicate != null) {
                                    DuplicateResolverDialog drd = new DuplicateResolverDialog
                                            (frame, duplicate, entry, DuplicateResolverDialog.IMPORT_CHECK);
                                    drd.setVisible(true);
                                    int answer = drd.getSelected();
                                    // int answer = DuplicateResolverDialog.resolveDuplicateInImport
                                    //        (frame, duplicate, entry);
                                    // The upper entry is the
                                    if (answer == DuplicateResolverDialog.DO_NOT_IMPORT) {
                                        keepEntry = false;
                                    } else if (answer == DuplicateResolverDialog.IMPORT_AND_DELETE_OLD) {
                                        remove = true;
                                        replaceCount++;
                                    } else if (answer == DuplicateResolverDialog.KEEP_MERGE) {
                                        keepEntry = false;
                                        remove = true;
                                        mergedEntry = drd.getMergedEntry();
                                        mergeCount++;
                                    } else if (answer == DuplicateResolverDialog.BREAK) {
                                        preString = Localization.lang("Import cancelled.");
                                        break mainLoop;
                                    }
                                }

                                // Remove the old entry, if we are supposed to
                                if (remove) {
                                 // Remove the old one and import the new one.
                                    toAddTo.removeEntry(duplicate.getId());
                                    ce.addEdit(new UndoableRemoveEntry(toAddTo, duplicate, panel));
                                }
                                // Add the entry, if we are supposed to:
                                if (keepEntry) {
                                    toAddTo.insertEntry(entry);
                                    // Generate key, if we are supposed to:
                                    if (generateKeys) {
                                        LabelPatternUtil.makeLabel(panel.metaData(), toAddTo, entry);
                                        //System.out.println("gen:"+entry.getCiteKey());
                                    }
                                    ce.addEdit(new UndoableInsertEntry(toAddTo, entry, panel));
                                    addCount++;
                                }
                                // Add the merged entry, if we are supposed to:
                                if (mergedEntry != null) {
                                    toAddTo.insertEntry(mergedEntry);
                                    // Generate key, if we are supposed to:
                                    if (generateKeys) {
                                        LabelPatternUtil.makeLabel(panel.metaData(), toAddTo, mergedEntry);
                                        //System.out.println("gen:"+entry.getCiteKey());
                                    }
                                    ce.addEdit(new UndoableInsertEntry(toAddTo, mergedEntry, panel));
                                }
                                
                            } catch (KeyCollisionException e) {
                                e.printStackTrace();
                            }
                        }
                        ce.end();
                        if (ce.hasEdits()) {
                            panel.undoManager.addEdit(ce);
                            panel.markBaseChanged();
                        }
                        if (preString != null) {
                            frame.output(preString + " " + Localization.lang("Added %0 entries. Replaced %1 entries. Added information to %2 entries.", addCount.toString(), replaceCount.toString(), mergeCount.toString()));
                        } else {
                            frame.output(Localization.lang("Added %0 entries. Replaced %1 entries. Added information to %2 entries.", addCount.toString(), replaceCount.toString(), mergeCount.toString()));
                        }
                    }

                }

                else {
                    frame.addTab(bibtexResult.getDatabase(), bibtexResult.getFile(),
                            bibtexResult.getMetaData(), Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING), true);
                    frame.output(Localization.lang("Imported entries") + ": " + bibtexResult.getDatabase().getEntryCount());
                }

            } else {
                if (importer == null) {
                    frame.output(Localization.lang("Could not find a suitable import format."));
                } else {
                    // Import in a specific format was specified. Check if we have stored error information:
                    if (importError != null) {
                        JOptionPane.showMessageDialog(frame, importError.getMessage(), Localization.lang("Import failed"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        JOptionPane.showMessageDialog(frame, Localization.lang("No entries found. Please make sure you are "
                                        + "using the correct import filter."), Localization.lang("Import failed"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            frame.unblock();
        }
    }


    private ParserResult mergeImportResults(List<ImportFormatReader.UnknownFormatImport> imports) {
        BibtexDatabase database = new BibtexDatabase();
        ParserResult directParserResult = null;
        boolean anythingUseful = false;

        for (ImportFormatReader.UnknownFormatImport importResult : imports) {
            if (importResult == null) {
                continue;
            }
            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format)) {
                // Bibtex result. We must merge it into our main base.
                ParserResult pr = importResult.parserResult;

                anythingUseful = anythingUseful
                        || (pr.getDatabase().getEntryCount() > 0) || (pr.getDatabase().getStringCount() > 0);

                // Record the parserResult, as long as this is the first bibtex result:
                if (directParserResult == null) {
                    directParserResult = pr;
                }

                // Merge entries:
                for (BibtexEntry entry : pr.getDatabase().getEntries()) {
                    database.insertEntry(entry);
                }

                // Merge strings:
                for (BibtexString bs : pr.getDatabase().getStringValues()) {
                    try {
                        database.addString((BibtexString) bs.clone());
                    } catch (KeyCollisionException e) {
                        // TODO: This means a duplicate string name exists, so it's not
                        // a very exceptional situation. We should maybe give a warning...?
                    }
                }
            } else {

                ParserResult pr = importResult.parserResult;
                Collection<BibtexEntry> entries = pr.getDatabase().getEntries();

                anythingUseful = anythingUseful | !entries.isEmpty();

                // set timestamp and owner
                Util.setAutomaticFields(entries, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                        Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP),
                        !openInNew && Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES)); // set timestamp and owner

                for (BibtexEntry entry : entries) {
                    database.insertEntry(entry);
                }
            }
        }

        if (!anythingUseful) {
            return null;
        }

        if ((imports.size() == 1) && (directParserResult != null)) {
            return directParserResult;
        } else {

            return new ParserResult(database, new MetaData(), new HashMap<String, BibtexEntryType>());

        }
    }

}
