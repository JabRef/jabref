package org.jabref.gui.preferences.journals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * a placeholder and it's path will be null meaning it has no place on the filesystem. It's isPseudoFile property
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
            List<Abbreviation> abbreviationList = JournalAbbreviationLoader.readJournalListFromFile(path.get());
            abbreviationList.forEach(abbreviation -> abbreviations.addAll(new AbbreviationViewModel(abbreviation)));
        } else {
            throw new FileNotFoundException();
        }
    }

    /**
     * This method will write all abbreviations of this abbreviation file to the file on the file system.
     * It essentially will check if the current file is a built in list and if not it will call
     * {@link AbbreviationWriter#writeOrCreate}.
     */
    public void writeOrCreate() throws IOException {
        if (!isBuiltInList.get()) {
            List<Abbreviation> actualAbbreviations =
                    abbreviations.stream().filter(abb -> !abb.isPseudoAbbreviation())
                                 .map(AbbreviationViewModel::getAbbreviationObject).collect(Collectors.toList());
            AbbreviationWriter.writeOrCreate(path.get(), actualAbbreviations, StandardCharsets.UTF_8);
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
        if (obj instanceof AbbreviationsFileViewModel) {
            return Objects.equals(this.name, ((AbbreviationsFileViewModel) obj).name);
        } else {
            return false;
        }
    }
}
