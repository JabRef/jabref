package org.jabref.gui.journals;

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

    private final Abbreviation abbreviationObject;
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty abbreviation = new SimpleStringProperty("");
    private final StringProperty shortestUniqueAbbreviation = new SimpleStringProperty("");
    private final BooleanProperty pseudoAbbreviation = new SimpleBooleanProperty();

    public AbbreviationViewModel(Abbreviation abbreviation) {
        this.abbreviationObject = abbreviation;
        this.pseudoAbbreviation.set(this.abbreviationObject == null);
        if (this.abbreviationObject != null) {
            this.name.bindBidirectional(this.abbreviationObject.nameProperty());
            this.abbreviation.bindBidirectional(this.abbreviationObject.abbreviationProperty());
            this.shortestUniqueAbbreviation.bindBidirectional(this.abbreviationObject.shortestUniqueAbbreviationProperty());
        }
    }

    public Abbreviation getAbbreviationObject() {
        return abbreviationObject;
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
    public int hashCode() {
        return Objects.hash(abbreviationObject);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbbreviationViewModel) {
            return Objects.equals(this.abbreviationObject, ((AbbreviationViewModel) obj).abbreviationObject);
        } else {
            return false;
        }
    }
}
