package org.jabref.gui.journals;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.journals.Abbreviation;

/**
 * This class provides a view model for abbreviation objects which can also
 * define placeholder objects of abbreviations. This is indicated by using the
 * {@code pseudoAbbreviation} property.
 */
public class AbbreviationViewModel {

    private final Abbreviation abbreviationObject;
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty abbreviation = new SimpleStringProperty("");
    private final BooleanProperty pseudoAbbreviation = new SimpleBooleanProperty();


    public AbbreviationViewModel(Abbreviation abbreviation) {
        this.abbreviationObject = abbreviation;
        pseudoAbbreviation.set(this.abbreviationObject == null);
        if (this.abbreviationObject != null) {
            this.name.bindBidirectional(this.abbreviationObject.nameProperty());
            this.abbreviation.bindBidirectional(this.abbreviationObject.abbreviationProperty());
        } else {
            this.name.set("Add new Abbreviation");
        }
    }

    public Abbreviation getAbbreviationObject() {
        return this.abbreviationObject;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation.set(abbreviation);
    }

    public String getName() {
        return this.name.get();
    }

    public String getAbbreviation() {
        return this.abbreviation.get();
    }

    public boolean isPseudoAbbreviation() {
        return this.pseudoAbbreviation.get();
    }

    public StringProperty nameProperty() {
        return this.name;
    }

    public StringProperty abbreviationProperty() {
        return this.abbreviation;
    }

    public BooleanProperty isPseudoAbbreviationProperty() {
        return this.pseudoAbbreviation;
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
