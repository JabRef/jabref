package spl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.imports.PdfContentImporter;
import net.sf.jabref.imports.PdfXmpImporter;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.util.XMPUtil;

import org.sciplore.beans.Document;

import spl.filter.PdfFileFilter;
import spl.gui.ImportDialog;
import spl.gui.MetaDataListDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 14:49:08
 * To change this template use File | Settings | File Templates.
 */
public class PdfImporter {

    private JabRefFrame frame;
    private BasePanel panel;
    private MainTable entryTable;
    private int dropRow;

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
     * @return list of non-PDF files
     */
    public String[] importPdfFiles(String[] fileNames, OutputPrinter status){
        List<String> files = new ArrayList<String>(Arrays.asList(fileNames));
        List<String> noPdfFiles = new ArrayList<String>();
        PdfFileFilter pdfFilter = new PdfFileFilter();
        for(String file : files){
            if(!pdfFilter.accept(file)){
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        importPdfFiles(files, status);
        String[] noPdfFilesArray = new String[noPdfFiles.size()];
        noPdfFiles.toArray(noPdfFilesArray);
        return noPdfFilesArray;
    }

    /**
     * @param fileNames - PDF files to import
     * @return true if the import succeeded, false otherwise
     */
    private boolean importPdfFiles(List<String> fileNames, OutputPrinter status){
        if(panel == null) return false;
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);

        // Get a list of file directories:
        ArrayList<File> dirs = new ArrayList<File>();
        String[] dirsS = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
        for (int i=0; i<dirsS.length; i++) {
            dirs.add(new File(dirsS[i]));
        }

        for(String fileName : fileNames){
            List<BibtexEntry> xmpEntriesInFile = readXmpEntries(fileName);
            if (!neverShow && !doNotShowAgain) {
            	importDialog = new ImportDialog(dropRow, fileName);
            	if(!hasXmpEntries(xmpEntriesInFile)){
                	importDialog.disableXMPChoice();
            	}
            	Tools.centerRelativeToWindow(importDialog, frame);
            	importDialog.showDialog();
            	doNotShowAgain = importDialog.getDoNotShowAgain();
            }
            if (neverShow || (importDialog.getResult() == JOptionPane.OK_OPTION)) {
                int choice = (neverShow?globalChoice:importDialog.getChoice());
            	DroppedFileHandler dfh;
            	BibtexEntry entry;
            	BibtexEntryType type;
                InputStream in = null;
                List<BibtexEntry> res = null;
            	MetaDataListDialog metaDataListDialog;
                switch (choice) {
    			case ImportDialog.XMP:
                    //SplDatabaseChangeListener dataListener = new SplDatabaseChangeListener(frame, panel, entryTable, fileName);
                    //panel.database().addDatabaseChangeListener(dataListener);
                    //ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
                    PdfXmpImporter importer = new PdfXmpImporter();
                    try {
                        in = new FileInputStream(fileName);
                        res = importer.importEntries(in, frame);
                        //importer.automatedImport(new String[]{ fileName });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try { in.close(); } catch (Exception f) {}
                    }

                    // import failed -> generate default entry
                    if ((res == null) || (res.size() == 0)) {
                        createNewBlankEntry(fileName);
                        return true;
                    }

                    // only one entry is imported
                    entry = res.get(0);

                    // insert entry to database and link file
                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    FileListTableModel tm = new FileListTableModel();
                    File toLink = new File(fileName);
                    tm.addEntry(0, new FileListEntry(toLink.getName(),
                            FileListEditor.relativizePath(toLink, dirs).getPath(),
                            Globals.prefs.getExternalFileTypeByName("pdf")));
                    entry.setField(GUIGlobals.FILE_FIELD, tm.getStringRepresentation());

			        break;

    			case ImportDialog.CONTENT:
                	PdfContentImporter contentImporter = new PdfContentImporter();
                	
                	File file = new File (fileName);

                	try {
						in = new FileInputStream(file);
					} catch (Exception e) {
						// import failed -> generate default entry
						Globals.logger(Globals.lang("Import failed"));
						e.printStackTrace();
						createNewBlankEntry(fileName);
						return true;
					}
					try {
						res = contentImporter.importEntries(in, status);
					} catch (Exception e) {
						// import failed -> generate default entry
						Globals.logger(Globals.lang("Import failed"));
						e.printStackTrace();
						createNewBlankEntry(fileName);
						return true;
					} finally {
						try { in.close(); } catch (Exception f) {}
					}
					
					// import failed -> generate default entry
					if ((res == null) || (res.size() == 0)) {
						createNewBlankEntry(fileName);
						return true;
					}
					
					// only one entry is imported
					entry = res.get(0);
					
					// insert entry to database and link file
					
                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    LabelPatternUtil.makeLabel(panel.metaData(), panel.database(), entry);
					dfh = new DroppedFileHandler(frame, panel);
					dfh.linkPdfToEntry(fileName, entryTable, entry);
                    panel.highlightEntry(entry);
                    if (Globals.prefs.getBoolean("autoOpenForm")) {
                        EntryEditor editor = panel.getEntryEditor(entry);
                        panel.showEntryEditor(editor);
                        panel.adjustSplitter();
                    }
                    break;
    			case ImportDialog.MRDLIB:
                    metaDataListDialog = new MetaDataListDialog(fileName, true);
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    Document document = metaDataListDialog.getXmlDocuments();
                    if(document != null /*&& documents.getDocuments() != null && documents.getDocuments().size() > 0*/ && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if(selected > -1 /*&& selected < documents.getDocuments().size()*/){
                            //Document document = documents/*.getDocuments().get(selected)*/;
                            String id = Util.createNeutralId();
                            entry = new BibtexEntry(id);
                            if(fieldExists(document.getType())){
                                type = BibtexEntryType.getStandardType(document.getType());
                                if(type == null){
                                    type = BibtexEntryType.ARTICLE;
                                }
                                entry.setType(type);
                            }
                            else{
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
                        else{
                            createNewBlankEntry(fileName);
                        }
                    }
                    else if(metaDataListDialog.getResult() == JOptionPane.CANCEL_OPTION ){
                        continue;
                    }
                    else if(metaDataListDialog.getResult() == JOptionPane.NO_OPTION ){
                        createNewBlankEntry(fileName);
                    }
                    else if(document == null /*|| document.getDocuments() == null || document.getDocuments().size() <= 0*/ && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        createNewBlankEntry(fileName);
                    }
                    break;
    			case ImportDialog.NOMETA:
                    createNewBlankEntry(fileName);
                    break;
    			case ImportDialog.UPDATEEMPTYFIELDS:
                    metaDataListDialog = new MetaDataListDialog(fileName, false);                   
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    document = metaDataListDialog.getXmlDocuments();
                    if(document != null /*&& document.getDocuments() != null && document.getDocuments().size() > 0*/ && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if(selected > -1 /*&& selected < document.getDocuments().size()*/){
                            //XmlDocument document = documents.getDocuments().get(selected);
                            entry = entryTable.getEntryAt(dropRow);
                            if(fieldExists(document.getType())){
                                type = BibtexEntryType.getStandardType(document.getType());
                                if(type != null){
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
        return true;
    }

    private void createNewBlankEntry(String fileName) {
        BibtexEntry newEntry = createNewEntry();
        if(newEntry != null){
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, entryTable, newEntry);
        }
    }

    private void insertFields(String[] fields, BibtexEntry entry, Document xmlDocument) {
        DocumentWrapper document = new DocumentWrapper(xmlDocument);
        for(String field : fields){
            if(entry.getField(field) != null){
                continue;
            }
            if(field.equalsIgnoreCase("author")){
                entry.setField(field, document.getAuthors("and"));
            }
            if(field.equalsIgnoreCase("title")){
                entry.setField(field, document.getTitle());
            }
            if(field.equalsIgnoreCase("abstract")){
                entry.setField(field, document.getAbstract());
            }
            /*if(field.equalsIgnoreCase("keywords")){
                entry.setField(field, document.getKeyWords());
            }*/
            if(field.equalsIgnoreCase("doi")){
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
            if(field.equalsIgnoreCase("year")){
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
            String id = Util.createNeutralId();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                panel.database().insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList<BibtexEntry> list = new ArrayList<BibtexEntry>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                panel.undoManager.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
                panel.output(Globals.lang("Added new")+" '"+type.getName().toLowerCase()+"' "
                       +Globals.lang("entry")+".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != panel.SHOWING_EDITOR) {
                    panel.setMode(panel.WILL_SHOW_EDITOR);
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
                Util.pr(ex.getMessage());
            }
        }
        return null;
    }

    private List<BibtexEntry> readXmpEntries(String fileName){
        List<BibtexEntry> xmpEntriesInFile = null;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (Exception e) {
           // Todo Logging
        }
        return xmpEntriesInFile;
    }

    private boolean hasXmpEntries(List<BibtexEntry> xmpEntriesInFile){
        if ((xmpEntriesInFile == null) || (xmpEntriesInFile.size() == 0)) {
            return false;
        }
        else{
            return true;
        }
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
