package spl;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.util.XMPUtil;
import org.sciplore.xml.XmlDocument;
import org.sciplore.xml.XmlDocuments;
import spl.filter.PdfFileFilter;
import spl.gui.ImportDialog;
import spl.gui.MetaDataListDialog;
import spl.listener.SplDatabaseChangeListener;

import javax.swing.*;
import java.io.File;
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

    public String[] importPdfFiles(String[] fileNames){
        List<String> files = new ArrayList<String>(Arrays.asList(fileNames));
        List<String> noPdfFiles = new ArrayList<String>();
        PdfFileFilter pdfFilter = new PdfFileFilter();
        for(String file : files){
            if(!pdfFilter.accept(file)){
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        importPdfFiles(files);
        String[] noPdfFilesArray = new String[noPdfFiles.size()];
        noPdfFiles.toArray(noPdfFilesArray);
        return noPdfFilesArray;
    }

    private boolean importPdfFiles(List<String> fileNames){
        if(panel == null) return false;
        for(String fileName : fileNames){
            List<BibtexEntry> xmpEntriesInFile = readXmpEntries(fileName);
            ImportDialog importDialog = new ImportDialog(dropRow, fileName);
            if(!hasXmpEntries(xmpEntriesInFile)){
                importDialog.getRadioButtonXmp().setEnabled(false);
            }
            Tools.centerRelativeToWindow(importDialog, frame);
            importDialog.showDialog();
            if(importDialog.getResult() == JOptionPane.OK_OPTION){
                if(importDialog.getRadioButtonXmp().isSelected()){
                    //SplDatabaseChangeListener dataListener = new SplDatabaseChangeListener(frame, panel, entryTable, fileName);
                    //panel.database().addDatabaseChangeListener(dataListener);
                    ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
			        importer.automatedImport(new String[]{ fileName });
                    
                }
                else if(importDialog.getRadioButtonMrDlib().isSelected()){                    
                    MetaDataListDialog metaDataListDialog = new MetaDataListDialog(fileName, true);
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    XmlDocuments documents = metaDataListDialog.getXmlDocuments();
                    if(documents != null && documents.getDocuments() != null && documents.getDocuments().size() > 0 && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if(selected > -1 && selected < documents.getDocuments().size()){
                            XmlDocument document = documents.getDocuments().get(selected);
                            String id = Util.createNeutralId();
                            BibtexEntry entry = new BibtexEntry(id);
                            if(fieldExists(document.getType())){
                                BibtexEntryType type = BibtexEntryType.getStandardType(document.getType());
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
                            insertFields(entry.getRequiredFields(), entry, document);
                            insertFields(entry.getGeneralFields(), entry, document);
                            insertFields(entry.getOptionalFields(), entry, document);
                            panel.database().insertEntry(entry);
                            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                            dfh.linkPdfToEntry(fileName, entryTable, entry);
                            LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), panel.database(), entry);
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
                    else if(documents == null || documents.getDocuments() == null || documents.getDocuments().size() <= 0 && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        createNewBlankEntry(fileName);
                    }
                }
                else if(importDialog.getRadioButtonNoMeta().isSelected()){
                    createNewBlankEntry(fileName);
                }
                else if(importDialog.getRadioButtonUpdateEmptyFields().isSelected()){
                    MetaDataListDialog metaDataListDialog = new MetaDataListDialog(fileName, false);                   
                    Tools.centerRelativeToWindow(metaDataListDialog, frame);
                    metaDataListDialog.showDialog();
                    XmlDocuments documents = metaDataListDialog.getXmlDocuments();
                    if(documents != null && documents.getDocuments() != null && documents.getDocuments().size() > 0 && metaDataListDialog.getResult() == JOptionPane.OK_OPTION){
                        int selected = metaDataListDialog.getTableMetadata().getSelectedRow();
                        if(selected > -1 && selected < documents.getDocuments().size()){
                            XmlDocument document = documents.getDocuments().get(selected);
                            BibtexEntry entry = entryTable.getEntryAt(dropRow);
                            if(fieldExists(document.getType())){
                                BibtexEntryType type = BibtexEntryType.getStandardType(document.getType());
                                if(type != null){
                                    entry.setType(type);
                                }
                            }
                            insertFields(entry.getRequiredFields(), entry, document);
                            insertFields(entry.getGeneralFields(), entry, document);
                            insertFields(entry.getOptionalFields(), entry, document);

                            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                            dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                        }
                    }
                }
                else if(importDialog.getRadioButtononlyAttachPDF().isSelected()){
                    DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, dropRow);
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

    private void insertFields(String[] fields, BibtexEntry entry, XmlDocument xmlDocument) {
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
            if(field.equalsIgnoreCase("keywords")){
                entry.setField(field, document.getKeyWords());
            }
            if(field.equalsIgnoreCase("doi")){
                entry.setField(field, document.getDoi());
            }
            if(field.equalsIgnoreCase("pages")){
                entry.setField(field, document.getPages());
            }
            if(field.equalsIgnoreCase("volume")){
                entry.setField(field, document.getVolume());
            }
            if(field.equalsIgnoreCase("number")){
                entry.setField(field, document.getNumber());
            }
            if(field.equalsIgnoreCase("year")){
                entry.setField(field, document.getYear());
            }
            if(field.equalsIgnoreCase("month")){
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
            }
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
