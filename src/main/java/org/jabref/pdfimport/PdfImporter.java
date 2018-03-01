package org.jabref.pdfimport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelMode;
import org.jabref.gui.EntryTypeDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiles.DroppedFileHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilShared;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfImporter.class);
    private final JabRefFrame frame;
    private final BasePanel panel;
    private final MainTable entryTable;

    private final int dropRow;

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

    /**
     *
     * Imports the PDF files given by fileNames
     *
     * @param fileNames states the names of the files to import
     * @return list of successful created BibTeX entries and list of non-PDF files
     */
    public ImportPdfFilesResult importPdfFiles(List<String> fileNames) {
        // sort fileNames in PDFfiles to import and other files
        // PDFfiles: variable files
        // other files: variable noPdfFiles
        List<String> files = new ArrayList<>(fileNames);
        List<String> noPdfFiles = new ArrayList<>();
        for (String file : files) {
            if (!PdfFileFilter.accept(file)) {
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        // files and noPdfFiles correctly sorted

        // import the files
        List<BibEntry> entries = importPdfFilesInternal(files);

        return new ImportPdfFilesResult(noPdfFiles, entries);
    }

    /**
     * @param fileNames - PDF files to import
     * @return true if the import succeeded, false otherwise
     */
    private List<BibEntry> importPdfFilesInternal(List<String> fileNames) {
        if (panel == null) {
            return Collections.emptyList();
        }
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(JabRefPreferences.IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE);

        List<BibEntry> res = new ArrayList<>();

        for (String fileName : fileNames) {
            if (!neverShow && !doNotShowAgain) {
                importDialog = new ImportDialog(dropRow >= 0, fileName);
                if (!XmpUtilShared.hasMetadata(Paths.get(fileName), Globals.prefs.getXMPPreferences())) {
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
                    createNewBlankEntry(fileName).ifPresent(res::add);
                    break;
                case ImportDialog.ONLYATTACH:
                    DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                    if (dropRow >= 0) {
                        dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                    } else {
                        dfh.linkPdfToEntry(fileName, entryTable, entryTable.getSelectedRow());
                    }
                    break;
                default:
                    break;
                }
            }

        }
        return res;
    }

    private void doXMPImport(String fileName, List<BibEntry> res) {
        List<BibEntry> localRes = new ArrayList<>();
        PdfXmpImporter importer = new PdfXmpImporter(Globals.prefs.getXMPPreferences());
        Path filePath = Paths.get(fileName);
        ParserResult result = importer.importDatabase(filePath, Globals.prefs.getDefaultEncoding());
        if (result.hasWarnings()) {
            frame.showMessage(result.getErrorMessage());
        }
        localRes.addAll(result.getDatabase().getEntries());

        if (localRes.isEmpty()) {
            // import failed -> generate default entry
            LOGGER.info("Import failed");
            createNewBlankEntry(fileName).ifPresent(res::add);
            return;
        }

        for (BibEntry entry : localRes) {
            // insert entry to database and link file
            panel.getDatabase().insertEntry(entry);
            panel.markBaseChanged();
            FileListTableModel tm = new FileListTableModel();
            Path toLink = Paths.get(fileName);
            // Get a list of file directories:
            List<Path> dirsS = panel.getBibDatabaseContext()
                    .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());

            tm.addEntry(0, new FileListEntry("", FileUtil.shortenFileName(toLink, dirsS).toString(),
                    ExternalFileTypes.getInstance().getExternalFileTypeByName("PDF")));
            entry.setField(FieldName.FILE, tm.getStringRepresentation());
            res.add(entry);
        }
    }

    private Optional<BibEntry> createNewBlankEntry(String fileName) {
        Optional<BibEntry> newEntry = createNewEntry();
        newEntry.ifPresent(bibEntry -> {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, bibEntry);
        });
        return newEntry;
    }

    private void doContentImport(String fileName, List<BibEntry> res) {

        PdfContentImporter contentImporter = new PdfContentImporter(
                Globals.prefs.getImportFormatPreferences());
        Path filePath = Paths.get(fileName);
        ParserResult result = contentImporter.importDatabase(filePath, Globals.prefs.getDefaultEncoding());
        if (result.hasWarnings()) {
            frame.showMessage(result.getErrorMessage());
        }

        if (!result.getDatabase().hasEntries()) {
            // import failed -> generate default entry
            createNewBlankEntry(fileName).ifPresent(res::add);
            return;
        }

        // only one entry is imported
        BibEntry entry = result.getDatabase().getEntries().get(0);

        // insert entry to database and link file
        panel.getDatabase().insertEntry(entry);
        panel.markBaseChanged();
        new BibtexKeyGenerator(panel.getBibDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences())
                .generateAndSetKey(entry);
        DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
        dfh.linkPdfToEntry(fileName, entry);

        SwingUtilities.invokeLater(() -> panel.highlightEntry(entry));

        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
            panel.showAndEdit(entry);
        }
        res.add(entry);
    }

    private Optional<BibEntry> createNewEntry() {
        // Find out what type is desired
        EntryTypeDialog etd = new EntryTypeDialog(frame);
        // We want to center the dialog, to make it look nicer.
        etd.setLocationRelativeTo(frame);
        etd.setVisible(true);
        EntryType type = etd.getChoice();

        if (type != null) { // Only if the dialog was not canceled.
            final BibEntry bibEntry = new BibEntry(type.getName());
            try {
                panel.getDatabase().insertEntry(bibEntry);

                // Set owner/timestamp if options are enabled:
                List<BibEntry> list = new ArrayList<>();
                list.add(bibEntry);
                UpdateField.setAutomaticFields(list, true, true, Globals.prefs.getUpdateFieldPreferences());

                // Create an UndoableInsertEntry object.
                panel.getUndoManager().addEdit(new UndoableInsertEntry(panel.getDatabase(), bibEntry, panel));
                panel.output(Localization.lang("Added new") + " '" + type.getName().toLowerCase(Locale.ROOT) + "' "
                        + Localization.lang("entry") + ".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != BasePanelMode.SHOWING_EDITOR) {
                    panel.setMode(BasePanelMode.WILL_SHOW_EDITOR);
                }

                SwingUtilities.invokeLater(() -> panel.showAndEdit(bibEntry));

                // The database just changed.
                panel.markBaseChanged();

                return Optional.of(bibEntry);
            } catch (KeyCollisionException ex) {
                LOGGER.info("Key collision occurred", ex);
            }
        }
        return Optional.empty();
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
}
