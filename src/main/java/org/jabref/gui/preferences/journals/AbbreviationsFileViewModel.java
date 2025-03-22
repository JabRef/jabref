package org.jabref.gui.preferences.journals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationWriter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationMvGenerator;

/**
 * This class provides a model for abbreviation files. It actually doesn't save the files as objects but rather saves
 * their paths. This also allows to specify pseudo files as placeholder objects.
 */
public class AbbreviationsFileViewModel {

    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final ReadOnlyBooleanProperty isBuiltInList;
    private final String name;
    private final Optional<Path> path;

    public AbbreviationsFileViewModel(Path filePath) {
        this.path = Optional.ofNullable(filePath);
        this.name = path.get().toAbsolutePath().toString();
        this.isBuiltInList = new SimpleBooleanProperty(false);
    }

    /**
     * This constructor should only be called to create a pseudo abbreviation file for built in lists. This means it is
     * a placeholder and its path will be null meaning it has no place on the filesystem. Its isPseudoFile property
     * will therefore be set to true.
     */
    public AbbreviationsFileViewModel(List<AbbreviationViewModel> abbreviations, String name) {
        this.abbreviations.addAll(abbreviations);
        this.name = name;
        this.path = Optional.empty();
        this.isBuiltInList = new SimpleBooleanProperty(true);
    }

    public void readAbbreviations() throws IOException {
        if (path.isPresent()) {
            Collection<Abbreviation> abbreviationList = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(path.get());
            abbreviationList.forEach(abbreviation -> abbreviations.addAll(new AbbreviationViewModel(abbreviation)));
        } else {
            throw new FileNotFoundException();
        }
    }
    /**
     * Reads journal abbreviations from the specified MV file and populates the abbreviations list.
     * <p>
     * If a valid file path is provided, this method loads abbreviations using
     * {@link JournalAbbreviationMvGenerator#loadAbbreviationsFromMv(Path)} and converts them
     * into {@link AbbreviationViewModel} objects before adding them to the  abbreviations list.
     * </p>
     *
     * @throws IOException if the MV file cannot be found or read.
     * @throws FileNotFoundException if no MV file path is specified.
     */

    public void readAbbreviationsFromMv() throws IOException {
        if (path.isPresent()) {
            // Load abbreviations from the MV file using MV processor.
            Collection<Abbreviation> abbreviationList = JournalAbbreviationMvGenerator.loadAbbreviationsFromMv(path.get());

            // Convert each Abbreviation into an AbbreviationViewModel and add it to the  abbreviations list.
            for (Abbreviation abbreviation : abbreviationList) {
                abbreviations.add(new AbbreviationViewModel(abbreviation));
            }
        } else {
            throw new FileNotFoundException("MV file not specified");
        }
    }

    /**
     * This method will write all abbreviations of this abbreviation file to the file on the file system.
     * It essentially will check if the current file is a builtin list and if not it will call
     * {@link AbbreviationWriter#writeOrCreate}.
     */
    public void writeOrCreate() throws IOException {
        if (!isBuiltInList.get() && !isMvFile()) {
            List<Abbreviation> actualAbbreviations =
                    abbreviations.stream().filter(abb -> !abb.isPseudoAbbreviation())
                                 .map(AbbreviationViewModel::getAbbreviationObject)
                                 .collect(Collectors.toList());
            AbbreviationWriter.writeOrCreate(path.get(), actualAbbreviations);
        }
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return abbreviations;
    }

    public boolean exists() {
        return path.isPresent() && Files.exists(path.get());
    }

    public Optional<Path> getAbsolutePath() {
        return path;
    }

    public ReadOnlyBooleanProperty isBuiltInListProperty() {
        return isBuiltInList;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbbreviationsFileViewModel model) {
            return Objects.equals(this.name, model.name);
        } else {
            return false;
        }
    }

    public boolean isMvFile() {
        return path.isPresent() && path.get().toString().endsWith(".mv");
    }
}
