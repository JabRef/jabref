package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationWriter;
import org.jabref.logic.journals.JournalAbbreviationLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a model for abbreviation files. It actually doesn't save the files as objects but rather saves
 * their paths. This also allows to specify pseudo files as placeholder objects.
 */
public class AbbreviationsFileViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationsFileViewModel.class);

    private final SimpleStringProperty name = new SimpleStringProperty();
    private final ReadOnlyBooleanWrapper isBuiltInList = new ReadOnlyBooleanWrapper();
    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Path filePath;
    private final SimpleBooleanProperty enabled = new SimpleBooleanProperty(true);

    /**
     * This creates a built in list containing the abbreviations from the given list
     *
     * @param name The name of the built in list
     */
    public AbbreviationsFileViewModel(List<AbbreviationViewModel> abbreviations, String name) {
        this.abbreviations.addAll(abbreviations);
        this.name.setValue(name);
        this.isBuiltInList.setValue(true);
        this.filePath = null;
    }

    public AbbreviationsFileViewModel(Path filePath) {
        this.name.setValue(filePath.getFileName().toString());
        this.filePath = filePath;
        this.isBuiltInList.setValue(false);
    }

    public boolean exists() {
        return isBuiltInList.get() || Files.exists(filePath);
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public ReadOnlyBooleanProperty isBuiltInListProperty() {
        return isBuiltInList.getReadOnlyProperty();
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return abbreviations;
    }

    /**
     * Reads journal abbreviations from the associated file and updates the abbreviations list.
     * For built-in lists, this method does nothing and returns immediately.
     * For file-based lists, it attempts to read abbreviations from the CSV file at the specified path.
     * If the file doesn't exist, a debug message is logged but no exception is propagated.
     *
     * @throws IOException If there is an error reading the abbreviation file
     */
    public void readAbbreviations() throws IOException {
        if (isBuiltInList.get()) {
            return;
        }
        try {
            Set<Abbreviation> abbreviationsFromFile = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(filePath);
            
            List<AbbreviationViewModel> viewModels = abbreviationsFromFile.stream()
                                                                   .map(AbbreviationViewModel::new)
                                                                   .collect(Collectors.toCollection(ArrayList::new));
            abbreviations.setAll(viewModels);
        } catch (NoSuchFileException e) {
            LOGGER.debug("Journal abbreviation list {} does not exist", filePath, e);
        }
    }

    /**
     * Writes the abbreviations to the associated file or creates a new file if it doesn't exist.
     * For built-in lists, this method does nothing and returns immediately.
     * For file-based lists, it collects all abbreviations from the property and writes them to the file.
     *
     * @throws IOException If there is an error writing to the file
     */
    public void writeOrCreate() throws IOException {
        if (isBuiltInList.get()) {
            return;
        }
        
        List<Abbreviation> abbreviationList = abbreviationsProperty().stream()
                                                                      .map(AbbreviationViewModel::getAbbreviationObject)
                                                                      .toList();
        AbbreviationWriter.writeOrCreate(filePath, abbreviationList);
    }

    /**
     * Gets the absolute path of this abbreviation file
     *
     * @return The optional absolute path of the file, or empty if it's a built-in list
     */
    public Optional<Path> getAbsolutePath() {
        if (isBuiltInList.get()) {
            return Optional.empty();
        }
        
        try {
            Path normalizedPath = filePath.toAbsolutePath().normalize();
            return Optional.of(normalizedPath);
        } catch (Exception e) {
            return Optional.of(filePath);
        }
    }
    
    /**
     * Checks if this source is enabled
     *
     * @return true if the source is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }
    
    /**
     * Sets the enabled state of this source
     *
     * @param enabled true to enable the source, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
    
    /**
     * Gets the enabled property for binding
     *
     * @return the enabled property
     */
    public BooleanProperty enabledProperty() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AbbreviationsFileViewModel viewModel = (AbbreviationsFileViewModel) o;
        if (isBuiltInList.get() && viewModel.isBuiltInList.get()) {
            return name.get().equals(viewModel.name.get());
        }
        return !isBuiltInList.get() && !viewModel.isBuiltInList.get() &&
                Objects.equals(filePath, viewModel.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isBuiltInList, filePath);
    }

    @Override
    public String toString() {
        return name.get();
    }
}

