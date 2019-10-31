package org.jabref.logic.journals;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.model.strings.StringUtil;

public class Abbreviation implements Comparable<Abbreviation> {

    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty abbreviation = new SimpleStringProperty("");
    private final SimpleStringProperty shortestUniqueAbbreviation = new SimpleStringProperty("");

    public Abbreviation(String name, String abbreviation) {
        this(name, abbreviation, StringUtil.EMPTY);
    }

    public Abbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        this.name.set(Objects.requireNonNull(name).trim());
        this.abbreviation.set(Objects.requireNonNull(abbreviation).trim());
        this.shortestUniqueAbbreviation.set(shortestUniqueAbbreviation.trim());
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation.get();
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation.set(abbreviation);
    }

    public SimpleStringProperty abbreviationProperty() {
        return abbreviation;
    }

    public String getShortestUniqueAbbreviation() {
        String result = shortestUniqueAbbreviation.get();
        if (result.isEmpty()) {
            return getAbbreviation();
        }
        return result;
    }

    public void setShortestUniqueAbbreviation(String shortestUniqueAbbreviation) {
        this.shortestUniqueAbbreviation.set(shortestUniqueAbbreviation);
    }

    public SimpleStringProperty shortestUniqueAbbreviationProperty() {
        return shortestUniqueAbbreviation;
    }

    public String getMedlineAbbreviation() {
        return getAbbreviation().replace(".", " ").replace("  ", " ").trim();
    }

    @Override
    public int compareTo(Abbreviation toCompare) {
        return getName().compareTo(toCompare.getName());
    }

    public String getNext(String current) {
        String currentTrimmed = current.trim();

        if (getMedlineAbbreviation().equals(currentTrimmed)) {
            return getShortestUniqueAbbreviation().equals(getAbbreviation()) ? getName() : getShortestUniqueAbbreviation();
        } else if (getShortestUniqueAbbreviation().equals(currentTrimmed) && !getShortestUniqueAbbreviation().equals(getAbbreviation())) {
            return getName();
        } else if (getName().equals(currentTrimmed)) {
            return getAbbreviation();
        } else {
            return getMedlineAbbreviation();
        }
    }

    @Override
    public String toString() {
        return String.format("Abbreviation{name=%s, abbreviation=%s, medlineAbbreviation=%s, shortestUniqueAbbreviation=%s}",
                this.name,
                this.abbreviation,
                this.getMedlineAbbreviation(),
                this.shortestUniqueAbbreviation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Abbreviation that = (Abbreviation) obj;

        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
