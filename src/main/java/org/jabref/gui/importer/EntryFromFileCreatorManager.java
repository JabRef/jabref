package org.jabref.gui.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CompoundEdit;

import org.jabref.gui.BasePanel;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IdGenerator;

/**
 * The class EntryFromFileCreatorManager manages entry creators.
 * The manager knows all existing implementations of the interface EntryFromFileCreator.
 * Given a file, the manager can then provide a creator, which is able to create a Bibtex entry for his file.
 * Knowing all implementations of the interface, the manager also knows the set of all files, of which Bibtex entries can be created.
 * The GUI uses this capability for offering the user only such files, of which entries could actually be created.
 * @author Dan&Nosh
 *
 */
public final class EntryFromFileCreatorManager {

    private final List<EntryFromFileCreator> entryCreators;


    public EntryFromFileCreatorManager(ExternalFileTypes externalFilesTypes) {

        entryCreators = new ArrayList<>(10);
        entryCreators.add(new EntryFromPDFCreator(externalFilesTypes));

        // add a creator for each ExternalFileType if there is no specialized
        // creator existing.
        Collection<ExternalFileType> fileTypes = externalFilesTypes.getExternalFileTypeSelection();

        for (ExternalFileType exFileType : fileTypes) {
            if (!hasSpecialisedCreatorForExternalFileType(exFileType)) {
                entryCreators.add(new EntryFromExternalFileCreator(exFileType));
            }
        }
    }

    private boolean hasSpecialisedCreatorForExternalFileType(
            ExternalFileType externalFileType) {
        for (EntryFromFileCreator entryCreator : entryCreators) {
            if ((entryCreator.getExternalFileType() == null)
                    || (entryCreator.getExternalFileType().getExtension().isEmpty())) {
                continue;
            }
            if (entryCreator.getExternalFileType().getExtension().equals(
                    externalFileType.getExtension())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a EntryFromFileCreator object that is capable of creating a
     * BibEntry for the given File.
     *
     * @param file the pdf file
     * @return null if there is no EntryFromFileCreator for this File.
     */
    public EntryFromFileCreator getEntryCreator(File file) {
        if ((file == null) || !file.exists()) {
            return null;
        }
        for (EntryFromFileCreator creator : entryCreators) {
            if (creator.accept(file)) {
                return creator;
            }
        }
        return null;
    }

    /**
     * Tries to add a entry for each file in the List.
     *
     * @param files
     * @param database
     * @param entryType
     * @return List of unexpected import event messages including failures.
     */
    public List<String> addEntrysFromFiles(List<File> files,
            BibDatabase database, EntryType entryType,
            boolean generateKeywordsFromPathToFile) {
        List<String> importGUIMessages = new LinkedList<>();
        addEntriesFromFiles(files, database, null, entryType,
                generateKeywordsFromPathToFile, null, importGUIMessages);
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
    public int addEntriesFromFiles(List<File> files,
            BibDatabase database, BasePanel panel, EntryType entryType,
            boolean generateKeywordsFromPathToFile,
            ChangeListener changeListener, List<String> importGUIMessages) {

        int count = 0;
        CompoundEdit ce = new CompoundEdit();
        for (File f : files) {
            EntryFromFileCreator creator = getEntryCreator(f);
            if (creator == null) {
                importGUIMessages.add("Problem importing " + f.getPath() + ": Unknown filetype.");
            } else {
                Optional<BibEntry> entry = creator.createEntry(f, generateKeywordsFromPathToFile);
                if (!entry.isPresent()) {
                    importGUIMessages.add("Problem importing " + f.getPath() + ": Entry could not be created.");
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
                        importGUIMessages.add("Problem importing " + f.getPath() + ": Insert into BibDatabase failed.");
                    } else {
                        count++;
                        if (panel != null) {
                            ce.addEdit(new UndoableInsertEntry(database, entry.get(), panel));
                        }
                    }
                }
            }

            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }

        if ((count > 0) && (panel != null)) {
            ce.end();
            panel.getUndoManager().addEdit(ce);
        }
        return count;

    }

    /**
     * Returns a {@link FileFilter} instance which will accept all files, for
     * which a {@link EntryFromFileCreator} exists, that accepts the files. <br>
     * <br>
     * This {@link FileFilter} will be displayed in the GUI as
     * "All supported files".
     *
     * @return A {@link FileFilter} that accepts all files for which creators
     *         exist.
     */
    private FileFilter getFileFilter() {
        return new FileFilter() {

            /**
             * Accepts all files, which are accepted by any known creator.
             */
            @Override
            public boolean accept(File file) {
                for (EntryFromFileCreator creator : entryCreators) {
                    if (creator.accept(file)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return Localization.lang("All external files");
            }
        };
    }

    /**
     * Returns a list of all {@link FileFilter} instances (i.e.
     * {@link EntryFromFileCreator}, plus the file filter that comes with the
     * {@link #getFileFilter()} method.
     *
     * @return A List of all known possible file filters.
     */
    public List<FileFilter> getFileFilterList() {

        List<FileFilter> filters = new ArrayList<>();
        filters.add(getFileFilter());
        for (FileFilter creator : entryCreators) {
            filters.add(creator);
        }
        return filters;
    }
}
