package net.sf.jabref.imports;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.Util;
import net.sf.jabref.external.ExternalFileType;

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

	
	private List<EntryFromFileCreator> entryCreators;

	
	
	public EntryFromFileCreatorManager() {
		
		entryCreators = new ArrayList<EntryFromFileCreator>(10);
		entryCreators.add(new EntryFromPDFCreator());
		
		// add a creator for each ExternalFileType if there is no specialised
		// creator existing.
        ExternalFileType[] fileTypes = JabRefPreferences.getInstance().getExternalFileTypeSelection();
         
		for (ExternalFileType exFileType : fileTypes) {
			if (!hasSpecialisedCreatorForExternalFileType(exFileType)) {
				entryCreators.add(new EntryFromExternalFileCreator(exFileType));
			}
		}
	}

	
	private boolean hasSpecialisedCreatorForExternalFileType(
			ExternalFileType externalFileType) {
		for (EntryFromFileCreator entryCreator : entryCreators) {
			if (entryCreator.getExternalFileType() == null || entryCreator.getExternalFileType().getExtension() == null) {
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
	 * BibtexEntry for the given File.
	 * 
	 * @param pdfFile
	 * @return null if there is no EntryFromFileCreator for this File.
	 */
	public EntryFromFileCreator getEntryCreator(File file) {
		if (file == null || !file.exists()) {
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
	 * Trys to add a entry for each file in the List.
	 * 
	 * @param files
	 * @param database
	 * @param entryType
	 * @param changeListener
	 * @return List of unexcpected import event messages including failures.
	 */
	public List<String> addEntrysFromFiles(List<File> files,
			BibtexDatabase database, BibtexEntryType entryType,
			boolean generateKeywordsFromPathToFile) {
		return addEntrysFromFiles(files, database, entryType,
				generateKeywordsFromPathToFile, null);
	}

	/**
	 * Tries to add a entry for each file in the List.
	 * 
	 * @param files
	 * @param database
	 * @param entryType
	 * @param generateKeywordsFromPathToFile
	 * @param changeListener
	 * @return Returns list of unexpected import event - Messages including
	 *         failures
	 */
	public List<String> addEntrysFromFiles(List<File> files,
			BibtexDatabase database, BibtexEntryType entryType,
			boolean generateKeywordsFromPathToFile,
			ChangeListener changeListener) {
		List<String> importGUIMessages = new LinkedList<String>();
		
		for (File f : files) {
			EntryFromFileCreator creator = getEntryCreator(f);
			if (creator != null) {
				BibtexEntry entry = creator.createEntry(f,
						generateKeywordsFromPathToFile);
				if (entry == null) {
					importGUIMessages.add("Problem importing " + f.getPath()
							+ ": Entry could not be created.");
					continue;
				}
				if (entryType != null) {
					entry.setType(entryType);
				}
				if (entry.getId() == null) {
					entry.setId(Util.createNeutralId());
				}
				/*
				 * TODO: database.insertEntry(BibtexEntry) is not sensible. Why
				 * does 'true' mean "There were duplicates", while 'false' means
				 * "Everything alright"?
				 */
				if (database.insertEntry(entry)) {
					importGUIMessages.add("Problem importing " + f.getPath()
							+ ": Insert into BibtexDatabase failed.");
				}
			} else {
				importGUIMessages.add("Problem importing " + f.getPath()
						+ ": Unknown filetype.");
			}

			if (changeListener != null)
				changeListener.stateChanged(new ChangeEvent(this));
		}
		return importGUIMessages;

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
	public FileFilter getFileFilter() {
		return new FileFilter() {

			/**
			 * Accepts all files, which are accepted by any known creator.
			 */
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
				return "All external files";
			}
		};
	}

	/**
	 * Returns a list of all {@link FileFilter} instances (i.e.
	 * {@link EntryFromFileCreator}, plus the file filter that comes with the
	 * {@link #getFileFilter()} method, plus the {@link EntryFromAnyFileCreator}
	 * file filter.
	 * 
	 * @return A List of all known possible file filters.
	 */
	public List<FileFilter> getFileFilterList() {
		

		List<FileFilter> filters = new ArrayList<FileFilter>();
		filters.add(getFileFilter());
		for (FileFilter creator : entryCreators) {
			filters.add(creator);
		}
		return filters;
	}
}
