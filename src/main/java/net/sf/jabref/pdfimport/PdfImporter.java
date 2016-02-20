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
package net.sf.jabref.pdfimport;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.model.entry.EntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.EntryTypeDialog;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.PdfContentImporter;
import net.sf.jabref.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

public class PdfImporter {

    private final JabRefFrame frame;
    private final BasePanel panel;
    private MainTable entryTable;
    private int dropRow;

    private static final Log LOGGER = LogFactory.getLog(PdfImporter.class);

    /**
     * Creates the PdfImporter
     *
     * @param frame the JabRef frame
     * @param panel the panel to use
     * @param entryTable the entry table to work on
     * @param dropRow the row the entry is dropped to. May be -1 to indicate that no row is selected.
     */
    public PdfImporter(JabRefFrame frame, BasePanel panel, MainTable entryTable, int dropRow) {
        this.frame = frame;
        this.panel = panel;
        this.entryTable = entryTable;
        this.dropRow = dropRow;
    }


    public class ImportPdfFilesResult {

        public String[] noPdfFiles;
        public List<BibEntry> entries;
    }


    /**
     *
     * Imports the PDF files given by fileNames
     *
     * @param fileNames states the names of the files to import
     * @return list of successful created BibTeX entries and list of non-PDF files
     */
    public ImportPdfFilesResult importPdfFiles(String[] fileNames, OutputPrinter status) {
        // sort fileNames in PDFfiles to import and other files
        // PDFfiles: variable files
        // other files: variable noPdfFiles
        List<String> files = new ArrayList<>(Arrays.asList(fileNames));
        List<String> noPdfFiles = new ArrayList<>();
        PdfFileFilter pdfFilter = PdfFileFilter.INSTANCE;
        for (String file : files) {
            if (!pdfFilter.accept(file)) {
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        // files and noPdfFiles correctly sorted

        // import the files
        List<BibEntry> entries = importPdfFiles(files, status);

        String[] noPdfFilesArray = new String[noPdfFiles.size()];
        noPdfFiles.toArray(noPdfFilesArray);

        ImportPdfFilesResult res = new ImportPdfFilesResult();
        res.noPdfFiles = noPdfFilesArray;
        res.entries = entries;
        return res;
    }

    /**
     * @param fileNames - PDF files to import
     * @return true if the import succeeded, false otherwise
     */
    private List<BibEntry> importPdfFiles(List<String> fileNames, OutputPrinter status) {
        if (panel == null) {
            return Collections.emptyList();
        }
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);

        // Get a list of file directories:
        List<String> dirsS = panel.getBibDatabaseContext().getMetaData().getFileDirectory(Globals.FILE_FIELD);

        List<BibEntry> res = new ArrayList<>();

        fileNameLoop: for (String fileName : fileNames) {
            List<BibEntry> xmpEntriesInFile = readXmpEntries(fileName);
            if (!neverShow && !doNotShowAgain) {
                importDialog = new ImportDialog(dropRow >= 0, fileName);
                if (!hasXmpEntries(xmpEntriesInFile)) {
                    importDialog.disableXMPChoice();
                }
                centerRelativeToWindow(importDialog, frame);
                importDialog.showDialog();
                doNotShowAgain = importDialog.isDoNotShowAgain();
            }
            if (neverShow || (importDialog.getResult() == JOptionPane.OK_OPTION)) {
                int choice = neverShow ? globalChoice : importDialog.getChoice();
                DroppedFileHandler dfh;
                BibEntry entry;
                InputStream in = null;
                List<BibEntry> localRes = null;
                switch (choice) {
                case ImportDialog.XMP:
                    PdfXmpImporter importer = new PdfXmpImporter();
                    try {
                        in = new FileInputStream(fileName);
                        localRes = importer.importEntries(in, frame);
                    } catch (IOException ex) {
                        LOGGER.warn("Cannot import entries", ex);
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                        } catch (IOException ignored) {
                            // Ignored
                        }
                    }

                    if ((localRes == null) || localRes.isEmpty()) {
                        // import failed -> generate default entry
                        LOGGER.info("Import failed");
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }

                    // only one entry is imported
                    entry = localRes.get(0);

                    // insert entry to database and link file
                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    FileListTableModel tm = new FileListTableModel();
                    File toLink = new File(fileName);
                    tm.addEntry(0, new FileListEntry(toLink.getName(),
                            FileUtil.shortenFileName(toLink, dirsS).getPath(),
                                    ExternalFileTypes.getInstance().getExternalFileTypeByName("pdf")));
                    entry.setField(Globals.FILE_FIELD, tm.getStringRepresentation());
                    res.add(entry);
                    break;

                case ImportDialog.CONTENT:
                    PdfContentImporter contentImporter = new PdfContentImporter();

                    File file = new File(fileName);

                    try {
                        in = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        // import failed -> generate default entry
                        LOGGER.info("Import failed", e);
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }
                    try {
                        localRes = contentImporter.importEntries(in, status);
                    } catch (IOException e) {
                        // import failed -> generate default entry
                        LOGGER.info("Import failed", e);
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    } finally {
                        try {
                            in.close();
                        } catch (IOException ignored) {
                            // Ignored
                        }
                    }

                    // import failed -> generate default entry
                    if ((localRes == null) || localRes.isEmpty()) {
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }

                    // only one entry is imported
                    entry = localRes.get(0);

                    // insert entry to database and link file

                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    LabelPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData(), panel.database(), entry);
                    dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entry);
                    panel.highlightEntry(entry);
                    if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                        EntryEditor editor = panel.getEntryEditor(entry);
                        panel.showEntryEditor(editor);
                        panel.adjustSplitter();
                    }
                    res.add(entry);
                    break;
                case ImportDialog.NOMETA:
                    entry = createNewBlankEntry(fileName);
                    res.add(entry);
                    break;
                case ImportDialog.ONLYATTACH:
                    dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                    break;
                }
            }

        }
        return res;
    }

    private BibEntry createNewBlankEntry(String fileName) {
        BibEntry newEntry = createNewEntry();
        if (newEntry != null) {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, newEntry);
        }
        return newEntry;
    }

    private BibEntry createNewEntry() {

        // Find out what type is wanted.
        EntryTypeDialog etd = new EntryTypeDialog(frame);
        // We want to center the dialog, to make it look nicer.
        PositionWindow.placeDialog(etd, frame);
        etd.setVisible(true);
        EntryType type = etd.getChoice();

        if (type != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibEntry be = new BibEntry(id, type.getName());
            try {
                panel.database().insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList<BibEntry> list = new ArrayList<>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                panel.undoManager.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
                panel.output(Localization.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Localization.lang("entry") + ".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != BasePanel.SHOWING_EDITOR) {
                    panel.setMode(BasePanel.WILL_SHOW_EDITOR);
                }

                /*int row = entryTable.findEntry(be);
                if (row >= 0)
                    // Selects the entry. The selection listener will open the editor.
                     if (row >= 0) {
                        try{
                            entryTable.setRowSelectionInterval(row, row);
                        }catch(IllegalArgumentException e){
                            System.out.println("RowCount: " + entryTable.getRowCount());
                        }

                        //entryTable.setActiveRow(row);
                        entryTable.ensureVisible(row);
                     }
                else {
                    // The entry is not visible in the table, perhaps due to a filtering search
                    // or group selection. Show the entry editor anyway:
                    panel.showEntry(be);
                }   */
                panel.showEntry(be);
                panel.markBaseChanged(); // The database just changed.
                new FocusRequester(panel.getEntryEditor(be));
                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info("Key collision occurred", ex);
            }
        }
        return null;
    }

    private static List<BibEntry> readXmpEntries(String fileName) {
        List<BibEntry> xmpEntriesInFile = null;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (IOException e) {
            LOGGER.error("XMPUtil.readXMP failed", e);
        }
        return xmpEntriesInFile;
    }

    private static boolean hasXmpEntries(List<BibEntry> xmpEntriesInFile) {
        return !((xmpEntriesInFile == null) || xmpEntriesInFile.isEmpty());
    }

    public MainTable getEntryTable() {
        return entryTable;
    }

    public void setEntryTable(MainTable entryTable) {
        this.entryTable = entryTable;
    }

    public int getDropRow() {
        return dropRow;
    }

    public void setDropRow(int dropRow) {
        this.dropRow = dropRow;
    }

    /**
     * Used nowhere else, will be removed at the JavaFX migration
     */
    private static void centerRelativeToWindow(java.awt.Dialog diag, java.awt.Container win) {
        int x;
        int y;

        Point topLeft = win.getLocationOnScreen();
        Dimension parentSize = win.getSize();

        Dimension mySize = diag.getSize();

        if (parentSize.width > mySize.width) {
            x = ((parentSize.width - mySize.width) / 2) + topLeft.x;
        } else {
            x = topLeft.x;
        }

        if (parentSize.height > mySize.height) {
            y = ((parentSize.height - mySize.height) / 2) + topLeft.y;
        } else {
            y = topLeft.y;
        }

        diag.setLocation(x, y);
    }

}
