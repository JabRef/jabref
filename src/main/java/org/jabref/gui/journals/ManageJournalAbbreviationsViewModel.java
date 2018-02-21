package org.jabref.gui.journals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a model for managing journal abbreviation lists.
 * It provides all necessary methods to create, modify or delete journal
 * abbreviations and files. To visualize the model one can bind the properties
 * to ui elements.
 */
public class ManageJournalAbbreviationsViewModel extends AbstractViewModel {

    private final Logger logger = LoggerFactory.getLogger(ManageJournalAbbreviationsViewModel.class);
    private final SimpleListProperty<AbbreviationsFileViewModel> journalFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleIntegerProperty abbreviationsCount = new SimpleIntegerProperty();
    private final SimpleObjectProperty<AbbreviationsFileViewModel> currentFile = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<AbbreviationViewModel> currentAbbreviation = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty isFileRemovable = new SimpleBooleanProperty();
    private final SimpleBooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isLoadingBuiltIn = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isLoadingIeee = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isAbbreviationEditableAndRemovable = new SimpleBooleanProperty();
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final JournalAbbreviationPreferences abbreviationsPreferences;
    private final JournalAbbreviationLoader journalAbbreviationLoader;
    private boolean shouldWriteLists = false;

    public ManageJournalAbbreviationsViewModel(PreferencesService preferences, DialogService dialogService, TaskExecutor taskExecutor, JournalAbbreviationLoader journalAbbreviationLoader) {
        this.preferences = Objects.requireNonNull(preferences);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.journalAbbreviationLoader = Objects.requireNonNull(journalAbbreviationLoader);
        this.abbreviationsPreferences = preferences.getJournalAbbreviationPreferences();

        abbreviationsCount.bind(abbreviations.sizeProperty());
        currentAbbreviation.addListener((observable, oldvalue, newvalue) -> {
            boolean isAbbreviation = (newvalue != null) && !newvalue.isPseudoAbbreviation();
            boolean isEditableFile = (currentFile.get() != null) && !currentFile.get().isBuiltInListProperty().get();
            isAbbreviationEditableAndRemovable.set(isAbbreviation && isEditableFile);
        });
        currentFile.addListener((observable, oldvalue, newvalue) -> {
            if (oldvalue != null) {
                abbreviations.unbindBidirectional(oldvalue.abbreviationsProperty());
                currentAbbreviation.set(null);
            }
            if (newvalue != null) {
                isFileRemovable.set(!newvalue.isBuiltInListProperty().get());
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
        journalFiles.addListener((ListChangeListener<AbbreviationsFileViewModel>) c -> {
            if (c.next()) {
                if (!c.wasReplaced()) {
                    if (c.wasAdded() && !c.getAddedSubList().get(0).isBuiltInListProperty().get()) {
                        currentFile.set(c.getAddedSubList().get(0));
                    }
                }
            }
        });
        isLoading.bind(isLoadingBuiltIn.and(isLoadingBuiltIn));
    }

    public SimpleBooleanProperty isLoadingProperty() {
        return isLoading;
    }

    public boolean isAbbreviationEditableAndRemovable() {
        return isAbbreviationEditableAndRemovable.get();
    }

    /**
     * This will wrap the built in and ieee abbreviations in pseudo abbreviation files
     * and add them to the list of journal abbreviation files.
     */
    void addBuiltInLists() {
        BackgroundTask
                .wrap(JournalAbbreviationLoader::getBuiltInAbbreviations)
                .onRunning(() -> isLoadingBuiltIn.setValue(true))
                .onSuccess(result -> {
                    isLoadingBuiltIn.setValue(false);
                    addList(Localization.lang("JabRef built in list"), result);
                })
                .onFailure(dialogService::showErrorDialogAndWait)
                .executeWith(taskExecutor);

        BackgroundTask
                .wrap(() -> {
                    if (abbreviationsPreferences.useIEEEAbbreviations()) {
                        return JournalAbbreviationLoader.getOfficialIEEEAbbreviations();
                    } else {
                        return JournalAbbreviationLoader.getStandardIEEEAbbreviations();
                    }
                })
                .onRunning(() -> isLoadingIeee.setValue(true))
                .onSuccess(result -> {
                    isLoadingIeee.setValue(false);
                    addList(Localization.lang("IEEE built in list"), result);
                })
                .onFailure(dialogService::showErrorDialogAndWait)
                .executeWith(taskExecutor);
    }

    private void addList(String name, List<Abbreviation> abbreviations) {
        List<AbbreviationViewModel> builtInListViewModel = new ArrayList<>();
        abbreviations.forEach(abbreviation -> builtInListViewModel.add(new AbbreviationViewModel(abbreviation)));
        AbbreviationsFileViewModel fileViewModel = new AbbreviationsFileViewModel(builtInListViewModel, name);
        journalFiles.add(fileViewModel);
    }

    /**
     * Read all saved file paths and read their abbreviations
     */
    public void createFileObjects() {
        List<String> externalFiles = abbreviationsPreferences.getExternalJournalLists();
        externalFiles.forEach(name -> openFile(Paths.get(name)));
    }

    /**
     * This method shall be used to add a new journal abbreviation file to the
     * set of journal abbreviation files. It basically just calls the
     * {@link #openFile(Path)}} method
     */
    public void addNewFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.TXT)
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration).ifPresent(this::openFile);
    }

    /**
     * Checks whether the file exists or if a new file should be opened.
     * The file will be added to the set of journal abbreviation files.
     * If the file also exists its abbreviations will be read and written
     * to the abbreviations property.
     *
     * @param filePath to the file
     */
    private void openFile(Path filePath) {
        AbbreviationsFileViewModel abbreviationsFile = new AbbreviationsFileViewModel(filePath);
        if (journalFiles.contains(abbreviationsFile)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal File"), Localization.lang("Journal file %s already added", filePath.toString()));
            return;
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

    public void openFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.TXT)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(this::openFile);
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
     * @param name         of the abbreviation object
     * @param abbreviation of the abbreviation object
     */
    public void addAbbreviation(String name, String abbreviation) {
        Abbreviation abbreviationObject = new Abbreviation(name, abbreviation);
        AbbreviationViewModel abbreviationViewModel = new AbbreviationViewModel(abbreviationObject);
        if (abbreviations.contains(abbreviationViewModel)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal Abbreviation"), Localization.lang("Abbreviation %s for journal %s already defined.", abbreviation, name));
        } else {
            abbreviations.add(abbreviationViewModel);
            currentAbbreviation.set(abbreviationViewModel);
            shouldWriteLists = true;
        }
    }

    /**
     * Method to change the currentAbbrevaition property to a new abbreviation.
     *
     * @param name         of the abbreviation object
     * @param abbreviation of the abbreviation object
     */
    public void editAbbreviation(String name, String abbreviation) {
        if (isAbbreviationEditableAndRemovable.get()) {
            Abbreviation abbreviationObject = new Abbreviation(name, abbreviation);
            AbbreviationViewModel abbViewModel = new AbbreviationViewModel(abbreviationObject);
            if (abbreviations.contains(abbViewModel)) {
                if (!abbViewModel.equals(currentAbbreviation.get())) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal Abbreviation"), Localization.lang("Abbreviation %s for journal %s already defined.", abbreviation, name));
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
     */
    private void setCurrentAbbreviationNameAndAbbreviationIfValid(String name, String abbreviation) {
        if (name.trim().isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Name cannot be empty"));
            return;
        } else if (abbreviation.trim().isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Abbreviation cannot be empty"));
            return;
        }
        currentAbbreviation.get().setName(name);
        currentAbbreviation.get().setAbbreviation(abbreviation);
        shouldWriteLists = true;
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
                shouldWriteLists = true;
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
    private void saveExternalFilesList() {
        List<String> extFiles = new ArrayList<>();
        journalFiles.forEach(file -> {
            if (!file.isBuiltInListProperty().get()) {
                file.getAbsolutePath().ifPresent(path -> extFiles.add(path.toAbsolutePath().toString()));
            }
        });
        abbreviationsPreferences.setExternalJournalLists(extFiles);
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
     * {@link #saveExternalFilesList()}, {@link #saveJournalAbbreviationFiles() } and finally
     * {@link JournalAbbreviationLoader#update(JournalAbbreviationPreferences)}.
     */
    public void saveEverythingAndUpdateAutoCompleter() {
        saveExternalFilesList();

        if (shouldWriteLists) {
            saveJournalAbbreviationFiles();
            shouldWriteLists = false;
        }

        // Update journal abbreviation loader
        journalAbbreviationLoader.update(abbreviationsPreferences);

        preferences.storeJournalAbbreviationPreferences(abbreviationsPreferences);
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

    public void addAbbreviation() {
        addAbbreviation(Localization.lang("Name"), Localization.lang("Abbreviation"));
    }

    public void init() {
        createFileObjects();
        selectLastJournalFile();
        addBuiltInLists();
    }
}
