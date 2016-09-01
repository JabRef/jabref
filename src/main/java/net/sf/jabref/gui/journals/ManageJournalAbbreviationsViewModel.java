/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.gui.journals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.DuplicatedJournalAbbreviationException;
import net.sf.jabref.logic.journals.DuplicatedJournalFileException;
import net.sf.jabref.logic.journals.EmptyFieldException;
import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a model for managing journal abbreviation lists.
 * It provides all necessary methods to create, modify or delete journal
 * abbreviations and files. To visualize the model one can bind the properties
 * to ui elements.
 */
public class ManageJournalAbbreviationsViewModel {

    private final Log logger = LogFactory.getLog(ManageJournalAbbreviationsViewModel.class);
    private final SimpleListProperty<AbbreviationsFileViewModel> journalFiles = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final SimpleIntegerProperty abbreviationsCount = new SimpleIntegerProperty();
    private final SimpleObjectProperty<AbbreviationsFileViewModel> currentFile = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<AbbreviationViewModel> currentAbbreviation = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty isFileRemovable = new SimpleBooleanProperty();
    private final SimpleBooleanProperty isAbbreviationEditableAndRemovable = new SimpleBooleanProperty();


    public ManageJournalAbbreviationsViewModel() {
        abbreviationsCount.bind(abbreviations.sizeProperty());
        currentAbbreviation.addListener((observable, oldvalue, newvalue) -> {
            isAbbreviationEditableAndRemovable.set(((newvalue == null) || newvalue.isPseudoAbbreviation()
                    || currentFile.get().isBuiltInListProperty().get()) ? false : true);
        });
        currentFile.addListener((observable, oldvalue, newvalue) -> {
            if (oldvalue != null) {
                abbreviations.unbindBidirectional(oldvalue.abbreviationsProperty());
                currentAbbreviation.set(null);
            }
            if (newvalue != null) {
                isFileRemovable.set(newvalue.isBuiltInListProperty().get() ? false : true);
                abbreviations.bindBidirectional(newvalue.abbreviationsProperty());
                if (abbreviations.size() > 0) {
                    currentAbbreviation.set(abbreviations.get(abbreviations.size() - 1));
                }
            } else {
                isFileRemovable.set(false);
                if (!journalFiles.isEmpty()) {
                    currentFile.set(journalFiles.get(0));
                } else {
                    currentAbbreviation.set(null);
                    abbreviations.clear();
                }
            }
        });
        journalFiles.addListener(new ListChangeListener<AbbreviationsFileViewModel>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends AbbreviationsFileViewModel> c) {
                if (c.next()) {
                    if (!c.wasReplaced()) {
                        if (c.wasAdded() && !c.getAddedSubList().get(0).isBuiltInListProperty().get()) {
                            currentFile.set(c.getAddedSubList().get(0));
                        }
                    }
                }
            }

        });
    }

    /**
     * This will wrap the built in and ieee abbreviations in pseudo abbreviation files
     * and add them to the list of journal abbreviation files.
     */
    public void addBuiltInLists() {
        List<Abbreviation> builtInList = Globals.journalAbbreviationLoader.getBuiltInAbbreviations();
        List<AbbreviationViewModel> builtInListViewModel = new ArrayList<>();
        builtInList.forEach(abbreviation -> builtInListViewModel.add(new AbbreviationViewModel(abbreviation)));
        AbbreviationsFileViewModel builtInFile = new AbbreviationsFileViewModel(builtInListViewModel,
                "JabRef built in list");
        List<Abbreviation> ieeeList;
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            ieeeList = Globals.journalAbbreviationLoader.getOfficialIEEEAbbreviations();
        } else {
            ieeeList = Globals.journalAbbreviationLoader.getStandardIEEEAbbreviations();
        }
        List<AbbreviationViewModel> ieeeListViewModel = new ArrayList<>();
        ieeeList.forEach(abbreviation -> ieeeListViewModel.add(new AbbreviationViewModel(abbreviation)));
        AbbreviationsFileViewModel ieeeFile = new AbbreviationsFileViewModel(ieeeListViewModel, "IEEE built in list");
        journalFiles.addAll(builtInFile, ieeeFile);
    }

    /**
     * Read all saved file paths and read their abbreviations
     */
    public void createFileObjects() {
        List<String> externalFiles = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        externalFiles.forEach(name -> {
            try {
                openFile(name);
            } catch (DuplicatedJournalFileException e) {
                logger.debug(e);
            }
        });
    }

    /**
     * This method shall be used to add a new journal abbreviation file to the
     * set of journal abbreviation files. It basically just calls the
     * {@link #openFile(File file)} method
     *
     * @param filePath where the new file should be written to
     * @throws DuplicatedJournalFileException if journal file with same path is already open
     */
    public void addNewFile(String filePath) throws DuplicatedJournalFileException {
        openFile(filePath);
    }

    /**
     * Checks whether the file exists or if a new file should be opened.
     * The file will be added to the set of journal abbreviation files.
     * If the file also exists its abbreviations will be read and written
     * to the abbreviations property.
     *
     * @param filePath to the file
     * @throws DuplicatedJournalFileException if journal file with same path is already open
     */
    public void openFile(String filePath) throws DuplicatedJournalFileException {
        AbbreviationsFileViewModel abbreviationsFile = new AbbreviationsFileViewModel(filePath);
        if (journalFiles.contains(abbreviationsFile)) {
            throw new DuplicatedJournalFileException("Duplicated Journal File");
        }
        if (abbreviationsFile.exists()) {
            try {
                abbreviationsFile.readAbbreviations();
            } catch (FileNotFoundException e) {
                logger.debug(e.getLocalizedMessage());
            }
        }
        journalFiles.add(abbreviationsFile);
    }

    /**
     * This method removes the currently selected file from the set of
     * journal abbreviation files. This will not remove existing files from
     * the file system. The {@code activeFile} property will always change
     * to the previous file in the {@code journalFiles} list property, except
     * the first file is selected. If so the next file will be selected except if
     * there are no more files than the {@code activeFile} property will be set
     * to {@code null}.
     */
    public void removeCurrentFile() {
        if (isFileRemovable.get()) {
            journalFiles.remove(currentFile.get());
            if (journalFiles.isEmpty()) {
                currentFile.set(null);
            }
        }
    }

    /**
     * Method to add a new abbreviation to the abbreviations list property.
     * It also sets the currentAbbreviation property to the new abbreviation.
     *
     * @param name of the abbreviation object
     * @param abbreviation of the abbreviation object
     * @throws DuplicatedJournalAbbreviationException if the abbreviation already exists
     */
    public void addAbbreviation(String name, String abbreviation) throws DuplicatedJournalAbbreviationException {
        Abbreviation abbreviationObject = new Abbreviation(name, abbreviation);
        AbbreviationViewModel abbreviationViewModel = new AbbreviationViewModel(abbreviationObject);
        if (abbreviations.contains(abbreviationViewModel)) {
            throw new DuplicatedJournalAbbreviationException("Duplicated journal abbreviation");
        } else {
            abbreviations.add(abbreviationViewModel);
            currentAbbreviation.set(abbreviationViewModel);
        }
    }

    /**
     * Method to change the currentAbbrevaition property to a new abbreviation.
     *
     * @param name of the abbreviation object
     * @param abbreviation of the abbreviation object
     * @throws EmptyFieldException if either the name or the abbreviation is empty
     * @throws DuplicatedJournalAbbreviationException if the abbreviation already exists
     */
    public void editAbbreviation(String name, String abbreviation)
            throws EmptyFieldException, DuplicatedJournalAbbreviationException {
        if (isAbbreviationEditableAndRemovable.get()) {
            Abbreviation abbreviationbject = new Abbreviation(name, abbreviation);
            AbbreviationViewModel abbViewModel = new AbbreviationViewModel(abbreviationbject);
            if (abbreviations.contains(abbViewModel)) {
                if (!abbViewModel.equals(currentAbbreviation.get())) {
                    throw new DuplicatedJournalAbbreviationException("Duplicated journal abbreviation");
                } else {
                    setCurrentAbbreviationNameAndAbbreviationIfValid(name, abbreviation);
                }
            } else {
                setCurrentAbbreviationNameAndAbbreviationIfValid(name, abbreviation);
            }
        }
    }

    /**
     * Sets the name and the abbreviation of the {@code currentAbbreviation} property
     * to the values of the {@code abbreviationsName} and {@code abbreviationsAbbreviation}
     * properties.
     *
     * @throws EmptyFieldException if either the name or the abbreviation is empty
     */
    private void setCurrentAbbreviationNameAndAbbreviationIfValid(String name, String abbreviation)
            throws EmptyFieldException {
        if (name.trim().isEmpty()) {
            throw new EmptyFieldException("Name can not be empty");
        } else if (abbreviation.trim().isEmpty()) {
            throw new EmptyFieldException("Abbrevaition can not be empty");
        }
        currentAbbreviation.get().setName(name);
        currentAbbreviation.get().setAbbreviation(abbreviation);
    }

    /**
     * Method to delete the abbreviation set in the currentAbbreviation property.
     * The currentAbbreviationProperty will be set to the previous or next abbreviation
     * in the abbreviations property if applicable. Else it will be set to {@code null}
     * if there are no abbreviations left.
     */
    public void deleteAbbreviation() {
        if (isAbbreviationEditableAndRemovable.get()) {
            if ((currentAbbreviation.get() != null) && !currentAbbreviation.get().isPseudoAbbreviation()) {
                int index = abbreviations.indexOf(currentAbbreviation.get());
                if (index > 1) {
                    currentAbbreviation.set(abbreviations.get(index - 1));
                } else if ((index + 1) < abbreviationsCount.get()) {
                    currentAbbreviation.set(abbreviations.get(index + 1));
                } else {
                    currentAbbreviation.set(null);
                }
                abbreviations.remove(index);
            }
        }
    }

    /**
     * Calls the {@link AbbreviationsFileViewModel#writeOrCreate()} method for each file
     * in the journalFiles property which will overwrite the existing files with
     * the content of the abbreviations property of the AbbriviationsFile. Non
     * existing files will be created.
     */
    public void saveJournalAbbreviationFiles() {
        journalFiles.forEach(file -> {
            try {
                file.writeOrCreate();
            } catch (IOException e) {
                logger.debug(e.getLocalizedMessage());
            }
        });
    }

    /**
     * This method stores all file paths of the files in the journalFiles property
     * to the global JabRef preferences. Pseudo abbreviation files will not be stored.
     */
    public void saveExternalFilesList() {
        List<String> extFiles = new ArrayList<>();
        journalFiles.forEach(file -> {
            if (!file.isBuiltInListProperty().get()) {
                file.getAbsolutePath().ifPresent(path -> extFiles.add(path.toAbsolutePath().toString()));
            }
        });
        Globals.prefs.putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, extFiles);
    }

    /**
     * This will set the {@code currentFile} property to the {@link AbbreviationsFileViewModel} object
     * that was added to the {@code journalFiles} list property lastly. If there are no files in the list
     * property this methode will do nothing as the {@code currentFile} property is already {@code null}.
     */
    public void selectLastJournalFile() {
        if (journalFiles.size() > 0) {
            currentFile.set(journalFilesProperty().get(journalFilesProperty().size() - 1));
        }
    }

    /**
     * This method first saves all external files to its internal list, then writes all abbreviations
     * to their files and finally updates the abbreviations auto complete. It basically calls
     * {@link #saveExternalFilesList()}, {@link #saveJournalAbbreviationFiles() }and finally
     * {@link #updateAbbreviationsAutoComplete()}.
     */
    public void saveEverythingAndUpdateAutoCompleter() {
        saveExternalFilesList();
        saveJournalAbbreviationFiles();
        // Update journal abbreviation loader
        Globals.journalAbbreviationLoader.update(JournalAbbreviationPreferences.fromPreferences(Globals.prefs));
    }

    public SimpleListProperty<AbbreviationsFileViewModel> journalFilesProperty() {
        return this.journalFiles;
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return this.abbreviations;
    }

    public SimpleIntegerProperty abbreviationsCountProperty() {
        return this.abbreviationsCount;
    }

    public SimpleObjectProperty<AbbreviationsFileViewModel> currentFileProperty() {
        return this.currentFile;
    }

    public SimpleObjectProperty<AbbreviationViewModel> currentAbbreviationProperty() {
        return this.currentAbbreviation;
    }

    public SimpleBooleanProperty isAbbreviationEditableAndRemovableProperty() {
        return this.isAbbreviationEditableAndRemovable;
    }

    public SimpleBooleanProperty isFileRemovableProperty() {
        return this.isFileRemovable;
    }

}
