package org.jabref.gui.preferences.journals;

import java.util.Locale;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.journals.Abbreviation;

/**
 * This class provides a view model for abbreviation objects which can also define placeholder objects of abbreviations.
 * This is indicated by using the {@code pseudoAbbreviation} property.
 */
public class AbbreviationViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty abbreviation = new SimpleStringProperty("");
    private final StringProperty shortestUniqueAbbreviation = new SimpleStringProperty("");
    private final BooleanProperty pseudoAbbreviation = new SimpleBooleanProperty();

    public AbbreviationViewModel(Abbreviation abbreviationObject) {
        this.pseudoAbbreviation.set(abbreviationObject == null);
        if (abbreviationObject != null) {
            this.name.setValue(abbreviationObject.getName());
            this.abbreviation.setValue(abbreviationObject.getAbbreviation());
            this.shortestUniqueAbbreviation.setValue(abbreviationObject.getShortestUniqueAbbreviation());
        }
    }

    public Abbreviation getAbbreviationObject() {
        return new Abbreviation(getName(), getAbbreviation(), getShortestUniqueAbbreviation());
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getAbbreviation() {
        return abbreviation.get();
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation.set(abbreviation);
    }

    public String getShortestUniqueAbbreviation() {
        return shortestUniqueAbbreviation.get();
    }

    public void setShortestUniqueAbbreviation(String shortestUniqueAbbreviation) {
        this.shortestUniqueAbbreviation.set(shortestUniqueAbbreviation);
    }

    public boolean isPseudoAbbreviation() {
        return pseudoAbbreviation.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty abbreviationProperty() {
        return abbreviation;
    }

    public StringProperty shortestUniqueAbbreviationProperty() {
        return shortestUniqueAbbreviation;
    }

    public BooleanProperty isPseudoAbbreviationProperty() {
        return pseudoAbbreviation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbbreviationViewModel that = (AbbreviationViewModel) o;
        return Objects.equals(getName(), that.getName()) &&
                isPseudoAbbreviation() == that.isPseudoAbbreviation();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), isPseudoAbbreviation());
    }

    public boolean containsCaseIndependent(String searchTerm) {
        searchTerm = searchTerm.toLowerCase(Locale.ROOT);
        return this.abbreviation.get().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                this.name.get().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                this.shortestUniqueAbbreviation.get().toLowerCase(Locale.ROOT).contains(searchTerm);
    }
}
