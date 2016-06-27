package net.sf.jabref.gui.importer;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CompoundEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.importer.EntryFromFileCreator;
import net.sf.jabref.importer.EntryFromFileCreatorManager;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;

public class EntryFromFileCreatorManagerGUI {

    private final EntryFromFileCreatorManager efcm;


    public EntryFromFileCreatorManagerGUI(EntryFromFileCreatorManager efcm) {
        this.efcm = efcm;
    }

    /**
     * Tries to add a entry for each file in the List.
     *
     * @param files
     * @param database
     * @param entryType
     * @return List of unexpected import event messages including failures.
     */
    public List<String> addEntrysFromFiles(List<File> files, BibDatabase database, EntryType entryType,
            boolean generateKeywordsFromPathToFile) {
        List<String> importGUIMessages = new LinkedList<>();
        addEntriesFromFiles(files, database, null, entryType, generateKeywordsFromPathToFile, null, importGUIMessages);
        return importGUIMessages;
    }

    /**
     * Tries to add a entry for each file in the List.
     *
     * @param files
     * @param database
     * @param panel
     * @param entryType
     * @param generateKeywordsFromPathToFile
     * @param changeListener
     * @param importGUIMessages list of unexpected import event - Messages including
     *         failures
     * @return Returns The number of entries added
     */
    public int addEntriesFromFiles(List<File> files, BibDatabase database, BasePanel panel, EntryType entryType,
            boolean generateKeywordsFromPathToFile, ChangeListener changeListener, List<String> importGUIMessages) {

        int count = 0;
        CompoundEdit compoundEdit = new CompoundEdit();
        for (File file : files) {
            EntryFromFileCreator creator = efcm.getEntryCreator(file);
            if (creator == null) {
                importGUIMessages.add("Problem importing " + file.getPath() + ": Unknown filetype.");
            } else {
                Optional<BibEntry> entry = creator.createEntry(file, generateKeywordsFromPathToFile);
                if (!entry.isPresent()) {
                    importGUIMessages.add("Problem importing " + file.getPath() + ": Entry could not be created.");
                    continue;
                }
                if (entryType != null) {
                    entry.get().setType(entryType);
                }
                if (entry.get().getId() == null) {
                    entry.get().setId(IdGenerator.next());
                }
                /*
                 * TODO: database.insertEntry(BibEntry) is not sensible. Why
                 * does 'true' mean "There were duplicates", while 'false' means
                 * "Everything alright"?
                 */
                if (!database.containsEntryWithId(entry.get().getId())) {
                    // Work around SIDE EFFECT of creator.createEntry. The EntryFromPDFCreator also creates the entry in the table
                    // Therefore, we only insert the entry if it is not already present
                    if (database.insertEntry(entry.get())) {
                        importGUIMessages
                                .add("Problem importing " + file.getPath() + ": Insert into BibDatabase failed.");
                    } else {
                        count++;
                        if (panel != null) {
                            compoundEdit.addEdit(new UndoableInsertEntry(database, entry.get(), panel));
                        }
                    }
                }
            }

            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }

        if ((count > 0) && (panel != null)) {
            compoundEdit.end();
            panel.getUndoManager().addEdit(compoundEdit);
        }
        return count;

    }

}
