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

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.journals.Abbreviation;
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
    private final SimpleListProperty<AbbreviationsFile> journalFiles = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final SimpleIntegerProperty abbreviationsCount = new SimpleIntegerProperty();
    private final SimpleStringProperty abbreviationsName = new SimpleStringProperty();
    private final SimpleStringProperty abbreviationsAbbreviation = new SimpleStringProperty();
    private final SimpleObjectProperty<AbbreviationsFile> currentFile = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<AbbreviationViewModel> currentAbbreviation = new SimpleObjectProperty<>();


    public ManageJournalAbbreviationsViewModel() {
        abbreviationsCount.bind(abbreviations.sizeProperty());
    }

    /**
     * This will wrap the built in and ieee abbreviations in pseudo abbreviation files
     * and add them to the list of journal abbreviation files.
     */
    public void addBuiltInLists() {
        List<Abbreviation> builtInList = Globals.journalAbbreviationLoader.getBuiltInAbbreviations();
        List<AbbreviationViewModel> builtInListViewModel = new ArrayList<>();
        builtInList.forEach(abbreviation -> builtInListViewModel.add(new AbbreviationViewModel(abbreviation)));
        AbbreviationsFile builtInFile = new AbbreviationsFile(builtInListViewModel, "JabRef built in list");
        journalFiles.add(builtInFile);
        List<Abbreviation> ieeeList;
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            ieeeList = Globals.journalAbbreviationLoader.getOfficialIEEEAbbreviations();
        } else {
            ieeeList = Globals.journalAbbreviationLoader.getStandardIEEEAbbreviations();
        }
        List<AbbreviationViewModel> ieeeListViewModel = new ArrayList<>();
        ieeeList.forEach(abbreviation -> ieeeListViewModel.add(new AbbreviationViewModel(abbreviation)));
        AbbreviationsFile ieeeFile = new AbbreviationsFile(ieeeListViewModel, "IEEE built in list");
        journalFiles.add(ieeeFile);
    }

    /**
     * Read all saved file paths and read their abbreviations
     */
    public void createFileObjects() {
        List<String> externalFiles = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        externalFiles.forEach(name -> {
            try {
                openFile(name);
            } catch (JabRefException e) {
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
     * @throws JabRefException if journal file with same path is already open
     */
    public void addNewFile(String filePath) throws JabRefException {
        openFile(filePath);
    }

    /**
     * Checks whether the file exists or if a new file should be opened.
     * The file will be added to the set of journal abbreviation files.
     * If the file also exists its abbreviations will be read and written
     * to the abbreviations property.
     *
     * @param filePath to the file
     * @throws JabRefException if journal file with same path is already open
     */
    public void openFile(String filePath) throws JabRefException {
        AbbreviationsFile abbreviationsFile = new AbbreviationsFile(filePath);
        if (journalFiles.contains(abbreviationsFile)) {
            throw new JabRefException("Duplicated Journal File");
        }
        journalFiles.add(abbreviationsFile);
        if (abbreviationsFile.exists()) {
            try {
                abbreviationsFile.readAbbreviations();
            } catch (FileNotFoundException e) {
                logger.debug(e.getLocalizedMessage());
            }
        }
    }

    /**
     * This method changes the currentFile property and binds its abbreviations list
     * to the abbreviations property of the view model.
     *
     * @param abbreviationsFile as new active file
     */
    public void changeActiveFile(AbbreviationsFile abbreviationsFile) {
        // unbind previous current file
        if (currentFile.get() != null) {
            abbreviations.unbindBidirectional(currentFile.get().abbreviationsProperty());
        }
        currentFile.set(abbreviationsFile);
        // set abbreviations property to abbreviations of current file
        if (abbreviationsFile != null) {
            abbreviationsProperty().bindBidirectional(abbreviationsFile.abbreviationsProperty());
            if (!abbreviations.isEmpty()) {
                currentAbbreviation.set(abbreviations.get(0));
            }
        } else {
            currentAbbreviation.set(null);
            abbreviations.clear();
        }
    }

    /**
     * This method removes the currently selected file from the set of
     * journal abbreviation files. This will not remove existing files from
     * the file system.
     */
    public void removeCurrentList() {
        int index = journalFiles.indexOf(currentFile.get());
        if (index > 0) {
            changeActiveFile(journalFiles.get(index - 1));
        } else if ((index + 1) < journalFiles.size()) {
            changeActiveFile(journalFiles.get(index + 1));
        } else {
            changeActiveFile(null);
        }
        journalFiles.remove(index);
    }

    /**
     * Method to add a new abbreviation to the abbreviations property.
     * The name and the actual abbreviation will be taken from the abbreviationsName
     * and abbreviationsAbbreviation properties. It also sets the currentAbbreviation
     * property to the new abbreviation.
     */
    public void addAbbreviation() throws JabRefException {
        Abbreviation abbreviation = new Abbreviation(abbreviationsName.get(), abbreviationsAbbreviation.get());
        AbbreviationViewModel abbreviationViewModel = new AbbreviationViewModel(abbreviation);
        if (abbreviations.contains(abbreviationViewModel)) {
            throw new JabRefException("Duplicated journal abbreviation");
        } else {
            abbreviations.add(abbreviationViewModel);
            currentAbbreviation.set(abbreviationViewModel);
        }
    }

    /**
     * Method to change the currentAbbrevaition property to a new abbreviation.
     * The name and the actual abbreviation will be taken from the abbreviationsName
     * and abbreviationsAbbreviation properties.
     *
     * @throws JabRefException
     */
    public void editAbbreviation() throws JabRefException {
        if (abbreviationsCount.get() != 0) {
            Abbreviation abbreviation = new Abbreviation(abbreviationsName.get(), abbreviationsAbbreviation.get());
            AbbreviationViewModel abbViewModel = new AbbreviationViewModel(abbreviation);
            if (abbreviations.contains(abbViewModel)) {
                if (abbViewModel.equals(currentAbbreviation.get())) {
                    setCurrentAbbreviationNameAndAbbreviationIfValid();
                } else {
                    throw new JabRefException("Duplicated journal abbreviation");
                }
            } else {
                setCurrentAbbreviationNameAndAbbreviationIfValid();
            }
        }
    }

    /**
     * Sets the name and the abbreviation of the {@code currentAbbreviation} property
     * to the values of the {@code abbreviationsName} and {@code abbreviationsAbbreviation}
     * properties. If the values are not valid for the abbreviation it will throw a {@link JabRefException}.
     *
     * @throws JabRefException
     */
    private void setCurrentAbbreviationNameAndAbbreviationIfValid() throws JabRefException {
        String name = abbreviationsName.get();
        String abbreviation = abbreviationsAbbreviation.get();
        if (name.trim().isEmpty()) {
            throw new JabRefException("Name can not be empty");
        } else if (abbreviation.trim().isEmpty()) {
            throw new JabRefException("Abbrevaition can not be empty");
        }
        currentAbbreviation.get().setName(name);
        currentAbbreviation.get().setAbbreviation(abbreviation);
    }

    /**
     * Method to delete the abbreviation set in the currentAbbreviation property.
     * The currentAbbreviationProperty will be set to the previous or next abbreviation
     * in the abbreviations property if applicable. Else it will be set null.
     */
    public void deleteAbbreviation() {
        int index = abbreviations.indexOf(currentAbbreviation.get());
        // index > 1 because there is a pseudo abbreviation at the start of every editable abbreviation list
        if (index > 1) {
            currentAbbreviation.set(abbreviations.get(index - 1));
        } else if ((index + 1) < abbreviationsCount.get()) {
            currentAbbreviation.set(abbreviations.get(index + 1));
        } else {
            currentAbbreviation.set(null);
        }
        abbreviations.remove(index);
    }

    /**
     * Calls the {@link AbbreviationsFile#WriteOrCreate()} method for each file
     * in the journalFiles property which will overwrite the existing files with
     * the content of the abbreviations property of the AbbriviationsFile. Non
     * existing files will be created.
     */
    public void saveJournalAbbreviationFiles() {
        journalFiles.forEach(file -> {
            try {
                file.WriteOrCreate();
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
            if (!file.isPseudoFileProperty().get()) {
                extFiles.add(file.getAbsolutePath());
            }
        });
        Globals.prefs.putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, extFiles);
    }

    /**
     * This will bind the given list property to the journal files list property of this class.
     * If the size of the given list property is bigger than 0 it will call
     * {@link #changeActiveFile(AbbreviationsFile)} on the first  item of the list.
     *
     * @param itemsProperty as list property of {@link AbbreviationsFile} objects
     */
    public void bindFileItems(SimpleListProperty<AbbreviationsFile> itemsProperty) {
        journalFiles.bindBidirectional(itemsProperty);
        if (!itemsProperty.isEmpty()) {
            changeActiveFile(journalFilesProperty().get(0));
        }
    }

    /**
     * <p><b>This method is untested!</b></p>
     * Should be called after the editing of the journal abbreviations has been
     * finished and the files have been saved with {@link #saveExternalFilesList()}
     * and {@link #saveJournalAbbreviationFiles()}.}
     */
    public void updateAbbreviationsAutoComplete() {
        // Update the autocompleter for the "journal" field in all base panels,
        // so added journal names are available:
        for (BasePanel basePanel : JabRefGUI.getMainFrame().getBasePanelList()) {
            basePanel.getAutoCompleters().addJournalListToAutoCompleter();
        }
    }

    public SimpleListProperty<AbbreviationsFile> journalFilesProperty() {
        return this.journalFiles;
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return this.abbreviations;
    }

    public SimpleStringProperty abbreviationsNameProperty() {
        return this.abbreviationsName;
    }

    public SimpleStringProperty abbreviationsAbbreviationProperty() {
        return this.abbreviationsAbbreviation;
    }

    public SimpleIntegerProperty abbreviationsCountProperty() {
        return this.abbreviationsCount;
    }

    public SimpleObjectProperty<AbbreviationsFile> currentFileProperty() {
        return this.currentFile;
    }

    public SimpleObjectProperty<AbbreviationViewModel> currentAbbreviationProperty() {
        return this.currentAbbreviation;
    }

}
