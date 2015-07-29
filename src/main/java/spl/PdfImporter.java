package spl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.imports.PdfContentImporter;
import net.sf.jabref.imports.PdfXmpImporter;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.util.FileUtil;
import net.sf.jabref.util.Util;
import net.sf.jabref.util.XMPUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sciplore.beans.Document;

import spl.filter.PdfFileFilter;
import spl.gui.ImportDialog;
import spl.gui.MetaDataListDialog;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 14:49:08
 * To change this template use File | Settings | File Templates.
 */
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
        public List<BibtexEntry> entries;
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
        List<String> files = new ArrayList<String>(Arrays.asList(fileNames));
        List<String> noPdfFiles = new ArrayList<String>();
        PdfFileFilter pdfFilter = new PdfFileFilter();
        for (String file : files) {
            if (!pdfFilter.accept(file)) {
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        // files and noPdfFiles correctly sorted

        // import the files
        List<BibtexEntry> entries = importPdfFiles(files, status);

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
    private List<BibtexEntry> importPdfFiles(List<String> fileNames, OutputPrinter status) {
        if (panel == null) {
            return Collections.emptyList();
        }
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);

        // Get a list of file directories:
        String[] dirsS = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);

        List<BibtexEntry> res = new ArrayList<BibtexEntry>();

        fileNameLoop: for (String fileName : fileNames) {
            List<BibtexEntry> xmpEntriesInFile = readXmpEntries(fileName);
            if (!neverShow && !doNotShowAgain) {
                importDialog = new ImportDialog(dropRow >= 0, fileName);
                if (!hasXmpEntries(xmpEntriesInFile)) {
                    importDialog.disableXMPChoice();
                }
                Tools.centerRelativeToWindow(importDialog, frame);
                importDialog.showDialog();
                doNotShowAgain = importDialog.getDoNotShowAgain();
            }
            if (neverShow || importDialog.getResult() == JOptionPane.OK_OPTION) {
                int choice = neverShow ? globalChoice : importDialog.getChoice();
                DroppedFileHandler dfh;
                BibtexEntry entry;
                BibtexEntryType type;
                InputStream in = null;
                List<BibtexEntry> localRes = null;
                MetaDataListDialog metaDataListDialog;
                switch (choice) {
                case ImportDialog.XMP:
                    //SplDatabaseChangeListener dataListener = new SplDatabaseChangeListener(frame, panel, entryTable, fileName);
                    //panel.database().addDatabaseChangeListener(dataListener);
                    //ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
                    PdfXmpImporter importer = new PdfXmpImporter();
                    try {
                        in = new FileInputStream(fileName);
                        localRes = importer.importEntries(in, frame);
                        //importer.automatedImport(new String[]{ fileName });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (Exception ignored) {
                        }
                    }

                    if (localRes == null || localRes.isEmpty()) {
                        // import failed -> generate default entry
                        LOGGER.info(Globals.lang("Import failed"));
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
                            Globals.prefs.getExternalFileTypeByName("pdf")));
                    entry.setField(GUIGlobals.FILE_FIELD, tm.getStringRepresentation());
                    res.add(entry);
                    break;

                case ImportDialog.CONTENT:
                    PdfContentImporter contentImporter = new PdfContentImporter();

                    File file = new File(fileName);

                    try {
                        in = new FileInputStream(file);
                    } catch (Exception e) {
                        // import failed -> generate default entry
                        LOGGER.info(Globals.lang("Import failed"), e);
                        e.printStackTrace();
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }
                    try {
                        localRes = contentImporter.importEntries(in, status);
                    } catch (Exception e) {
                        // import failed -> generate default entry
                        LOGGER.info(Globals.lang("Import failed"), e);
                        e.printStackTrace();
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    } finally {
                        try {
                            in.close();
                        } catch (Exception ignored) {
                        }
                    }

                    // import failed -> generate default entry
                    if (localRes == null || localRes.isEmpty()) {
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }

                    // only one entry is imported
                    entry = localRes.get(0);

                    // insert entry to database and link file

                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    LabelPatternUtil.makeLabel(panel.metaData(), panel.database(), entry);
                    dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, entry);
                    panel.highlightEntry(entry);
                    if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                        EntryEditor editor = panel.getEntryEditor(entry);
                        panel.showEntryEditor(editor);
                        panel.adjustSplitter();
                    }
                    res.add(entry);
                    break;
                case ImportDialog.MRDLIB:
                    metaDataListDialog = new MetaDataListDialog(fileName, true);
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    Document document = metaDataListDialog.getXmlDocuments();
                    entry = null; // to satisfy the Java compiler
                    if (document != null && metaDataListDialog.getResult() == JOptionPane.OK_OPTION) {
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if (selected > -1 /*&& selected < documents.getDocuments().size()*/) {
                            //Document document = documents/*.getDocuments().get(selected)*/;
                            String id = IdGenerator.next();
                            entry = new BibtexEntry(id);
                            if (fieldExists(document.getType())) {
                                type = BibtexEntryType.getStandardType(document.getType());
                                if (type == null) {
                                    type = BibtexEntryType.ARTICLE;
                                }
                                entry.setType(type);
                            }
                            else {
                                entry.setType(BibtexEntryType.ARTICLE);
                            }
                            ArrayList<BibtexEntry> list = new ArrayList<BibtexEntry>();
                            list.add(entry);
                            Util.setAutomaticFields(list, true, true, false);
                            //insertFields(entry.getRequiredFields(), entry, document);
                            insertFields(BibtexFields.getAllFieldNames(), entry, document);
                            //insertFields(entry.getOptionalFields(), entry, document);
                            panel.database().insertEntry(entry);
                            dfh = new DroppedFileHandler(frame, panel);
                            dfh.linkPdfToEntry(fileName, entryTable, entry);
                            LabelPatternUtil.makeLabel(panel.metaData(), panel.database(), entry);
                        }
                        else {
                            entry = createNewBlankEntry(fileName);
                        }
                    }
                    else if (metaDataListDialog.getResult() == JOptionPane.CANCEL_OPTION) {
                        continue;
                    }
                    else if (metaDataListDialog.getResult() == JOptionPane.NO_OPTION) {
                        entry = createNewBlankEntry(fileName);
                    }
                    else if (document == null && metaDataListDialog.getResult() == JOptionPane.OK_OPTION) {
                        entry = createNewBlankEntry(fileName);
                    }
                    assert entry != null;
                    res.add(entry);
                    break;
                case ImportDialog.NOMETA:
                    entry = createNewBlankEntry(fileName);
                    res.add(entry);
                    break;
                case ImportDialog.UPDATEEMPTYFIELDS:
                    metaDataListDialog = new MetaDataListDialog(fileName, false);
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    document = metaDataListDialog.getXmlDocuments();
                    if (document != null && metaDataListDialog.getResult() == JOptionPane.OK_OPTION) {
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if (selected > -1 /*&& selected < document.getDocuments().size()*/) {
                            //XmlDocument document = documents.getDocuments().get(selected);
                            entry = entryTable.getEntryAt(dropRow);
                            if (fieldExists(document.getType())) {
                                type = BibtexEntryType.getStandardType(document.getType());
                                if (type != null) {
                                    entry.setType(type);
                                }
                            }
                            //insertFields(entry.getRequiredFields(), entry, document);
                            insertFields(BibtexFields.getAllFieldNames(), entry, document);
                            //insertFields(entry.getOptionalFields(), entry, document);

                            dfh = new DroppedFileHandler(frame, panel);
                            dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                        }
                    }
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

    private BibtexEntry createNewBlankEntry(String fileName) {
        BibtexEntry newEntry = createNewEntry();
        if (newEntry != null) {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, entryTable, newEntry);
        }
        return newEntry;
    }

    private void insertFields(String[] fields, BibtexEntry entry, Document xmlDocument) {
        DocumentWrapper document = new DocumentWrapper(xmlDocument);
        for (String field : fields) {
            if (entry.getField(field) != null) {
                continue;
            }
            if (field.equalsIgnoreCase("author")) {
                entry.setField(field, document.getAuthors("and"));
            }
            if (field.equalsIgnoreCase("title")) {
                entry.setField(field, document.getTitle());
            }
            if (field.equalsIgnoreCase("abstract")) {
                entry.setField(field, document.getAbstract());
            }
            /*if(field.equalsIgnoreCase("keywords")){
                entry.setField(field, document.getKeyWords());
            }*/
            if (field.equalsIgnoreCase("doi")) {
                entry.setField(field, document.getDoi());
            }
            /*if(field.equalsIgnoreCase("pages")){
                entry.setField(field, document.getPages());
            }
            if(field.equalsIgnoreCase("volume")){
                entry.setField(field, document.getVolume());
            }
            if(field.equalsIgnoreCase("number")){
                entry.setField(field, document.getNumber());
            }*/
            if (field.equalsIgnoreCase("year")) {
                entry.setField(field, document.getYear());
            }
            /*if(field.equalsIgnoreCase("month")){
                entry.setField(field, document.getMonth());
            }
            if(field.equalsIgnoreCase("day")){
                entry.setField(field, document.getDay());
            }
            if(field.equalsIgnoreCase("booktitle")){
                entry.setField(field, document.getVenue());
            }
            if(field.equalsIgnoreCase("journal")){
                entry.setField(field, document.getVenue());
            }*/
        }
    }

    private boolean fieldExists(String string) {
        return string != null && !string.isEmpty();
    }

    private BibtexEntry createNewEntry() {

        // Find out what type is wanted.
        EntryTypeDialog etd = new EntryTypeDialog(frame);
        // We want to center the dialog, to make it look nicer.
        Util.placeDialog(etd, frame);
        etd.setVisible(true);
        BibtexEntryType type = etd.getChoice();

        if (type != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                panel.database().insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList<BibtexEntry> list = new ArrayList<BibtexEntry>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                panel.undoManager.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
                panel.output(Globals.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Globals.lang("entry") + ".");

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
                LOGGER.info("Key collision occured", ex);
            }
        }
        return null;
    }

    private List<BibtexEntry> readXmpEntries(String fileName) {
        List<BibtexEntry> xmpEntriesInFile = null;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (Exception e) {
            // Todo Logging
        }
        return xmpEntriesInFile;
    }

    private boolean hasXmpEntries(List<BibtexEntry> xmpEntriesInFile) {
        return !(xmpEntriesInFile == null || xmpEntriesInFile.isEmpty());
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
