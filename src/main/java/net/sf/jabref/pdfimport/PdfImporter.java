package net.sf.jabref.pdfimport;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.BasePanelMode;
import net.sf.jabref.gui.EntryTypeDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.externalfiles.DroppedFileHandler;
import net.sf.jabref.gui.externalfiletype.ExternalFileTypes;
import net.sf.jabref.gui.filelist.FileListEntry;
import net.sf.jabref.gui.filelist.FileListTableModel;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.PdfContentImporter;
import net.sf.jabref.logic.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PdfImporter {

    private final JabRefFrame frame;
    private final BasePanel panel;
    private final MainTable entryTable;
    private final int dropRow;

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
                if (!XMPUtil.hasMetadata(Paths.get(fileName), Globals.prefs.getXMPPreferences())) {
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
        List<BibEntry> localRes = new ArrayList<>();
        PdfXmpImporter importer = new PdfXmpImporter(Globals.prefs.getXMPPreferences());
        Path filePath = Paths.get(fileName);
        ParserResult result = importer.importDatabase(filePath, Globals.prefs.getDefaultEncoding());
        if (result.hasWarnings()) {
            frame.showMessage(result.getErrorMessage());
        }
        localRes.addAll(result.getDatabase().getEntries());

        BibEntry entry;
        if (localRes.isEmpty()) {
            // import failed -> generate default entry
            LOGGER.info("Import failed");
            createNewBlankEntry(fileName).ifPresent(res::add);
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
        List<String> dirsS = panel.getBibDatabaseContext()
                .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());

        tm.addEntry(0, new FileListEntry(toLink.getName(), FileUtil.shortenFileName(toLink, dirsS).getPath(),
                ExternalFileTypes.getInstance().getExternalFileTypeByName("PDF")));
        entry.setField(FieldName.FILE, tm.getStringRepresentation());
        res.add(entry);

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
        BibtexKeyPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData()
                .getCiteKeyPattern(Globals.prefs.getBibtexKeyPatternPreferences().getKeyPattern()), panel.getDatabase(), entry,
                Globals.prefs.getBibtexKeyPatternPreferences());
        DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
        dfh.linkPdfToEntry(fileName, entry);
        panel.highlightEntry(entry);
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
            EntryEditor editor = panel.getEntryEditor(entry);
            panel.showEntryEditor(editor);
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
            String id = IdGenerator.next();
            final BibEntry bibEntry = new BibEntry(id, type.getName());
            try {
                panel.getDatabase().insertEntry(bibEntry);

                // Set owner/timestamp if options are enabled:
                List<BibEntry> list = new ArrayList<>();
                list.add(bibEntry);
                UpdateField.setAutomaticFields(list, true, true, Globals.prefs.getUpdateFieldPreferences());

                // Create an UndoableInsertEntry object.
                panel.getUndoManager().addEdit(new UndoableInsertEntry(panel.getDatabase(), bibEntry, panel));
                panel.output(Localization.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Localization.lang("entry") + ".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != BasePanelMode.SHOWING_EDITOR) {
                    panel.setMode(BasePanelMode.WILL_SHOW_EDITOR);
                }

                SwingUtilities.invokeLater(() -> panel.showEntry(bibEntry));

                // The database just changed.
                panel.markBaseChanged();

                return Optional.of(bibEntry);
            } catch (KeyCollisionException ex) {
                LOGGER.info("Key collision occurred", ex);
            }
        }
        return Optional.empty();
    }
}
