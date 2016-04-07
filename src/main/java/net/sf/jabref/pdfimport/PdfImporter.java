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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
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
import net.sf.jabref.gui.BasePanelMode;
import net.sf.jabref.gui.EntryTypeDialog;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.importer.fileformat.PdfContentImporter;
import net.sf.jabref.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;

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

        private final List<String> noPdfFiles;
        private final List<BibEntry> entries;


        public ImportPdfFilesResult(List<String> noPdfFiles, List<BibEntry> entries) {
            this.noPdfFiles = noPdfFiles;
            this.entries = entries;
        }


        public List<String> getNoPdfFiles() {
            return noPdfFiles;
        }


        public List<BibEntry> getEntries() {
            return entries;
        }
    }


    /**
     *
     * Imports the PDF files given by fileNames
     *
     * @param fileNames states the names of the files to import
     * @return list of successful created BibTeX entries and list of non-PDF files
     */
    public ImportPdfFilesResult importPdfFiles(String[] fileNames) {
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
        List<BibEntry> entries = importPdfFiles(files);

        return new ImportPdfFilesResult(noPdfFiles, entries);
    }

    /**
     * @param fileNames - PDF files to import
     * @return true if the import succeeded, false otherwise
     */
    private List<BibEntry> importPdfFiles(List<String> fileNames) {
        if (panel == null) {
            return Collections.emptyList();
        }
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);


        List<BibEntry> res = new ArrayList<>();

        for (String fileName : fileNames) {
            if (!neverShow && !doNotShowAgain) {
                importDialog = new ImportDialog(dropRow >= 0, fileName);
                if (!XMPUtil.hasMetadata(Paths.get(fileName))) {
                    importDialog.disableXMPChoice();
                }
                importDialog.setLocationRelativeTo(frame);
                importDialog.showDialog();
                doNotShowAgain = importDialog.isDoNotShowAgain();
            }
            if (neverShow || (importDialog.getResult() == JOptionPane.OK_OPTION)) {
                int choice = neverShow ? globalChoice : importDialog.getChoice();
                switch (choice) {
                case ImportDialog.XMP:
                    doXMPImport(fileName, res);
                    break;

                case ImportDialog.CONTENT:
                    doContentImport(fileName, res);
                    break;
                case ImportDialog.NOMETA:
                    BibEntry entry = createNewBlankEntry(fileName);
                    res.add(entry);
                    break;
                case ImportDialog.ONLYATTACH:
                    DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                    break;
                default:
                    break;
                }
            }

        }
        return res;
    }

    private void doXMPImport(String fileName, List<BibEntry> res) {
        BibEntry entry;
        List<BibEntry> localRes = new ArrayList<>();
        PdfXmpImporter importer = new PdfXmpImporter();
        try (InputStream in = new FileInputStream(fileName)) {
            localRes.addAll(importer.importEntries(in, frame));
        } catch (IOException ex) {
            LOGGER.warn("Cannot import entries", ex);
        }

        if (localRes.isEmpty()) {
            // import failed -> generate default entry
            LOGGER.info("Import failed");
            entry = createNewBlankEntry(fileName);
            res.add(entry);
            return;
        }

        // only one entry is imported
        entry = localRes.get(0);

        // insert entry to database and link file
        panel.getDatabase().insertEntry(entry);
        panel.markBaseChanged();
        FileListTableModel tm = new FileListTableModel();
        File toLink = new File(fileName);
        // Get a list of file directories:
        List<String> dirsS = panel.getBibDatabaseContext().getFileDirectory();

        tm.addEntry(0, new FileListEntry(toLink.getName(), FileUtil.shortenFileName(toLink, dirsS).getPath(),
                ExternalFileTypes.getInstance().getExternalFileTypeByName("PDF")));
        entry.setField(Globals.FILE_FIELD, tm.getStringRepresentation());
        res.add(entry);

    }
    private BibEntry createNewBlankEntry(String fileName) {
        BibEntry newEntry = createNewEntry();
        if (newEntry != null) {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, newEntry);
        }
        return newEntry;
    }

    private void doContentImport(String fileName, List<BibEntry> res) {
        File file = new File(fileName);
        BibEntry entry;
        try (InputStream in = new FileInputStream(file)) {
            PdfContentImporter contentImporter = new PdfContentImporter();
            List<BibEntry> localRes = contentImporter.importEntries(in, frame);

            if (localRes.isEmpty()) {
                // import failed -> generate default entry
                entry = createNewBlankEntry(fileName);
                res.add(entry);
                return;
            }

            // only one entry is imported
            entry = localRes.get(0);
        } catch (IOException e) {
            // import failed -> generate default entry
            LOGGER.info("Import failed", e);
            entry = createNewBlankEntry(fileName);
            res.add(entry);
            return;
        }

        // insert entry to database and link file
        panel.getDatabase().insertEntry(entry);
        panel.markBaseChanged();
        LabelPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData(), panel.getDatabase(), entry);
        DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
        dfh.linkPdfToEntry(fileName, entry);
        panel.highlightEntry(entry);
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
            EntryEditor editor = panel.getEntryEditor(entry);
            panel.showEntryEditor(editor);
            panel.adjustSplitter();
        }
        res.add(entry);
    }

    private BibEntry createNewEntry() {
        // Find out what type is desired
        EntryTypeDialog etd = new EntryTypeDialog(frame);
        // We want to center the dialog, to make it look nicer.
        etd.setLocationRelativeTo(frame);
        etd.setVisible(true);
        EntryType type = etd.getChoice();

        if (type != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibEntry be = new BibEntry(id, type.getName());
            try {
                panel.getDatabase().insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList<BibEntry> list = new ArrayList<>();
                list.add(be);
                UpdateField.setAutomaticFields(list, true, true);

                // Create an UndoableInsertEntry object.
                panel.undoManager.addEdit(new UndoableInsertEntry(panel.getDatabase(), be, panel));
                panel.output(Localization.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Localization.lang("entry") + ".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != BasePanelMode.SHOWING_EDITOR) {
                    panel.setMode(BasePanelMode.WILL_SHOW_EDITOR);
                }

                panel.showEntry(be);

                // The database just changed.
                panel.markBaseChanged();

                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info("Key collision occurred", ex);
            }
        }
        return null;
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


}
