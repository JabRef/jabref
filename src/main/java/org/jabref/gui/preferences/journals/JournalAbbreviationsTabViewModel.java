package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a model for managing journal abbreviation lists. It provides all necessary methods to create,
 * modify or delete journal abbreviations and files. To visualize the model one can bind the properties to UI elements.
 */
public class JournalAbbreviationsTabViewModel implements PreferenceTabViewModel {

    private final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationsTabViewModel.class);

    private final SimpleListProperty<AbbreviationsFileViewModel> journalFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleIntegerProperty abbreviationsCount = new SimpleIntegerProperty();

    private final SimpleObjectProperty<AbbreviationsFileViewModel> currentFile = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<AbbreviationViewModel> currentAbbreviation = new SimpleObjectProperty<>();

    private final SimpleBooleanProperty isFileRemovable = new SimpleBooleanProperty();
    private final SimpleBooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isEditableAndRemovable = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isAbbreviationEditableAndRemovable = new SimpleBooleanProperty(false);

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final JournalAbbreviationPreferences abbreviationsPreferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private boolean shouldWriteLists;

    public JournalAbbreviationsTabViewModel(PreferencesService preferences,
                                            DialogService dialogService,
                                            TaskExecutor taskExecutor,
                                            JournalAbbreviationRepository journalAbbreviationRepository) {
        this.preferences = Objects.requireNonNull(preferences);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.journalAbbreviationRepository = Objects.requireNonNull(journalAbbreviationRepository);
        this.abbreviationsPreferences = preferences.getJournalAbbreviationPreferences();

        abbreviationsCount.bind(abbreviations.sizeProperty());
        currentAbbreviation.addListener((observable, oldValue, newValue) -> {
            boolean isAbbreviation = (newValue != null) && !newValue.isPseudoAbbreviation();
            boolean isEditableFile = (currentFile.get() != null) && !currentFile.get().isBuiltInListProperty().get();
            isEditableAndRemovable.set(isEditableFile);
            isAbbreviationEditableAndRemovable.set(isAbbreviation && isEditableFile);
        });
        currentFile.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                abbreviations.unbindBidirectional(oldValue.abbreviationsProperty());
                currentAbbreviation.set(null);
            }
            if (newValue != null) {
                isFileRemovable.set(!newValue.isBuiltInListProperty().get());
                abbreviations.bindBidirectional(newValue.abbreviationsProperty());
                if (!abbreviations.isEmpty()) {
                    currentAbbreviation.set(abbreviations.get(abbreviations.size() - 1));
                }
            } else {
                isFileRemovable.set(false);
                if (journalFiles.isEmpty()) {
                    currentAbbreviation.set(null);
                    abbreviations.clear();
                } else {
                    currentFile.set(journalFiles.get(0));
                }
            }
        });
        journalFiles.addListener((ListChangeListener<AbbreviationsFileViewModel>) lcl -> {
            if (lcl.next()) {
                if (!lcl.wasReplaced()) {
                    if (lcl.wasAdded() && !lcl.getAddedSubList().get(0).isBuiltInListProperty().get()) {
                        currentFile.set(lcl.getAddedSubList().get(0));
                    }
                }
            }
        });
    }

    @Override
    public void setValues() {
        journalFiles.clear();

        createFileObjects();
        selectLastJournalFile();
        addBuiltInList();
    }

    /**
     * Read all saved file paths and read their abbreviations.
     */
    public void createFileObjects() {
        List<String> externalFiles = abbreviationsPreferences.getExternalJournalLists();
        externalFiles.forEach(name -> openFile(Path.of(name)));
    }

    /**
     * This will set the {@code currentFile} property to the {@link AbbreviationsFileViewModel} object that was added to
     * the {@code journalFiles} list property lastly. If there are no files in the list property this method will do
     * nothing as the {@code currentFile} property is already {@code null}.
     */
    public void selectLastJournalFile() {
        if (!journalFiles.isEmpty()) {
            currentFile.set(journalFilesProperty().get(journalFilesProperty().size() - 1));
        }
    }

    /**
     * This will load the built in abbreviation files and add it to the list of journal abbreviation files.
     */
    public void addBuiltInList() {
        BackgroundTask
                .wrap(journalAbbreviationRepository::getAllLoaded)
                .onRunning(() -> isLoading.setValue(true))
                .onSuccess(result -> {
                    isLoading.setValue(false);
                    List<AbbreviationViewModel> builtInViewModels = result.stream()
                                                                          .map(AbbreviationViewModel::new)
                                                                          .collect(Collectors.toList());
                    journalFiles.add(new AbbreviationsFileViewModel(builtInViewModels, Localization.lang("JabRef built in list")));
                    selectLastJournalFile();
                })
                .onFailure(dialogService::showErrorDialogAndWait)
                .executeWith(taskExecutor);
    }

    /**
     * This method shall be used to add a new journal abbreviation file to the set of journal abbreviation files. It
     * basically just calls the {@link #openFile(Path)}} method.
     */
    public void addNewFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSV)
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration).ifPresent(this::openFile);
    }

    /**
     * Checks whether the file exists or if a new file should be opened. The file will be added to the set of journal
     * abbreviation files. If the file also exists its abbreviations will be read and written to the abbreviations
     * property.
     *
     * @param filePath path to the file
     */
    private void openFile(Path filePath) {
        AbbreviationsFileViewModel abbreviationsFile = new AbbreviationsFileViewModel(filePath);
        if (journalFiles.contains(abbreviationsFile)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal File"),
                    Localization.lang("Journal file %s already added", filePath.toString()));
            return;
        }
        if (abbreviationsFile.exists()) {
            try {
                abbreviationsFile.readAbbreviations();
            } catch (IOException e) {
                LOGGER.debug(e.getLocalizedMessage());
            }
        }
        journalFiles.add(abbreviationsFile);
    }

    public void openFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSV)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(this::openFile);
    }

    /**
     * This method removes the currently selected file from the set of journal abbreviation files. This will not remove
     * existing files from the file system. The {@code activeFile} property will always change to the previous file in
     * the {@code journalFiles} list property, except the first file is selected. If so the next file will be selected
     * except if there are no more files than the {@code activeFile} property will be set to {@code null}.
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
     * Method to add a new abbreviation to the abbreviations list property. It also sets the currentAbbreviation
     * property to the new abbreviation.
     *
     * @param name                       name of the abbreviation
     * @param abbreviation               default abbreviation of the abbreviation
     * @param shortestUniqueAbbreviation shortest unique abbreviation of the abbreviation
     */
    public void addAbbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        Abbreviation abbreviationObject = new Abbreviation(name, abbreviation, shortestUniqueAbbreviation);
        AbbreviationViewModel abbreviationViewModel = new AbbreviationViewModel(abbreviationObject);
        if (abbreviations.contains(abbreviationViewModel)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal Abbreviation"),
                    Localization.lang("Abbreviation '%0' for journal '%1' already defined.", abbreviation, name));
        } else {
            abbreviations.add(abbreviationViewModel);
            currentAbbreviation.set(abbreviationViewModel);
            shouldWriteLists = true;
        }
    }

    public void addAbbreviation(String name, String abbreviation) {
        addAbbreviation(name, abbreviation, "");
    }

    public void addAbbreviation() {
        addAbbreviation(
                Localization.lang("Name"),
                Localization.lang("Abbreviation"),
                Localization.lang("Shortest unique abbreviation"));
    }

    /**
     * Method to change the currentAbbreviation property to a new abbreviation.
     *
     * @param name                       name of the abbreviation
     * @param abbreviation               default abbreviation of the abbreviation
     * @param shortestUniqueAbbreviation shortest unique abbreviation of the abbreviation
     */
    public void editAbbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        if (isEditableAndRemovable.get()) {
            Abbreviation abbreviationObject = new Abbreviation(name, abbreviation, shortestUniqueAbbreviation);
            AbbreviationViewModel abbViewModel = new AbbreviationViewModel(abbreviationObject);
            if (abbreviations.contains(abbViewModel)) {
                if (abbViewModel.equals(currentAbbreviation.get())) {
                    setCurrentAbbreviationNameAndAbbreviationIfValid(name, abbreviation, shortestUniqueAbbreviation);
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("Duplicated Journal Abbreviation"),
                            Localization.lang("Abbreviation '%0' for journal '%1' already defined.", abbreviation, name));
                }
            } else {
                setCurrentAbbreviationNameAndAbbreviationIfValid(name, abbreviation, shortestUniqueAbbreviation);
            }
        }
    }

    public void editAbbreviation(String name, String abbreviation) {
        editAbbreviation(name, abbreviation, "");
    }

    /**
     * Sets the name and the abbreviation of the {@code currentAbbreviation} property to the values of the {@code name},
     * {@code abbreviation}, and {@code shortestUniqueAbbreviation} properties.
     *
     * @param name                       name of the abbreviation
     * @param abbreviation               default abbreviation of the abbreviation
     * @param shortestUniqueAbbreviation shortest unique abbreviation of the abbreviation
     */
    private void setCurrentAbbreviationNameAndAbbreviationIfValid(String name, String abbreviation, String shortestUniqueAbbreviation) {
        if (name.trim().isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Name cannot be empty"));
            return;
        }
        if (abbreviation.trim().isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Abbreviation cannot be empty"));
            return;
        }
        currentAbbreviation.get().setName(name);
        currentAbbreviation.get().setAbbreviation(abbreviation);
        currentAbbreviation.get().setShortestUniqueAbbreviation(shortestUniqueAbbreviation);
        shouldWriteLists = true;
    }

    /**
     * Method to delete the abbreviation set in the currentAbbreviation property. The currentAbbreviationProperty will
     * be set to the previous or next abbreviation in the abbreviations property if applicable. Else it will be set to
     * {@code null} if there are no abbreviations left.
     */
    public void deleteAbbreviation() {
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

    public void removeAbbreviation(AbbreviationViewModel abbreviation) {
        Objects.requireNonNull(abbreviation);

        if (abbreviation.isPseudoAbbreviation()) {
            return;
        }

        abbreviations.remove(abbreviation);
        shouldWriteLists = true;
    }

    /**
     * Calls the {@link AbbreviationsFileViewModel#writeOrCreate()} method for each file in the journalFiles property
     * which will overwrite the existing files with the content of the abbreviations property of the AbbreviationsFile.
     * Non existing files will be created.
     */
    public void saveJournalAbbreviationFiles() {
        journalFiles.forEach(file -> {
            try {
                file.writeOrCreate();
            } catch (IOException e) {
                LOGGER.debug(e.getLocalizedMessage());
            }
        });
    }

    /**
     * This method first saves all external files to its internal list, then writes all abbreviations to their files and
     * finally updates the abbreviations auto complete.
     */
    @Override
    public void storeSettings() {
        BackgroundTask
                .wrap(() -> {
                    List<String> journalStringList = journalFiles.stream()
                                                                 .filter(path -> !path.isBuiltInListProperty().get())
                                                                 .filter(path -> path.getAbsolutePath().isPresent())
                                                                 .map(path -> path.getAbsolutePath().get().toAbsolutePath().toString())
                                                                 .collect(Collectors.toList());

                    preferences.storeJournalAbbreviationPreferences(new JournalAbbreviationPreferences(
                            journalStringList,
                            abbreviationsPreferences.getDefaultEncoding()
                    ));

                    if (shouldWriteLists) {
                        saveJournalAbbreviationFiles();
                        shouldWriteLists = false;
                    }
                })
                .onSuccess((success) -> Globals.journalAbbreviationRepository =
                        JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences()))
                .onFailure(exception -> LOGGER.error("Failed to store journal preferences.", exception))
                .executeWith(taskExecutor);
    }

    public SimpleBooleanProperty isLoadingProperty() {
        return isLoading;
    }

    public SimpleListProperty<AbbreviationsFileViewModel> journalFilesProperty() {
        return journalFiles;
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return abbreviations;
    }

    public SimpleIntegerProperty abbreviationsCountProperty() {
        return abbreviationsCount;
    }

    public SimpleObjectProperty<AbbreviationsFileViewModel> currentFileProperty() {
        return currentFile;
    }

    public SimpleObjectProperty<AbbreviationViewModel> currentAbbreviationProperty() {
        return currentAbbreviation;
    }

    public SimpleBooleanProperty isEditableAndRemovableProperty() {
        return isEditableAndRemovable;
    }

    public SimpleBooleanProperty isAbbreviationEditableAndRemovable() {
        return isAbbreviationEditableAndRemovable;
    }

    public SimpleBooleanProperty isFileRemovableProperty() {
        return isFileRemovable;
    }
}
