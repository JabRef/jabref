package org.jabref.logic.journals;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;

public class Abbreviation implements Comparable<Abbreviation> {

    private static final String SPLITTER = ";"; // elements after SPLITTER are not used at the moment

    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty abbreviation = new SimpleStringProperty("");
    private final SimpleStringProperty shortestUnique = new SimpleStringProperty("");

    public Abbreviation(String name, String abbreviation, String shortestUnique) {
        this.name.set(Objects.requireNonNull(name).trim());
        this.abbreviation.set(Objects.requireNonNull(abbreviation).trim());
        this.shortestUnique.set(shortestUnique.trim());
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
        return this.abbreviation.get();
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation.set(abbreviation);
    }

    public SimpleStringProperty abbreviationProperty() {
        return abbreviation;
    }

    public String getShortestUnique() {
        return this.shortestUnique.get();
    }

    public void setShortestUnique(String shortestUnique) {
        this.shortestUnique.set(shortestUnique);
    }

    public SimpleStringProperty shortestUniqueProperty() {
        return shortestUnique;
    }

    public String getIsoAbbreviation() {
        if (getAbbreviation().contains(SPLITTER)) {
            String[] restParts = getAbbreviation().split(SPLITTER);
            return restParts[0].trim();
        }
        return getAbbreviation();
    }

    public String getMedlineAbbreviation() {
        return getIsoAbbreviation().replace(".", " ").replace("  ", " ").trim();
    }

    @Override
    public int compareTo(Abbreviation toCompare) {
        return getName().compareTo(toCompare.getName());
    }

    public String getNext(String current) {
        String currentTrimmed = current.trim();

        if (getShortestUnique().equals(currentTrimmed)) {
            return getName();
        } else if (getName().equals(currentTrimmed)) {
            return getIsoAbbreviation();
        } else if (getIsoAbbreviation().equals(currentTrimmed)) {
            return getMedlineAbbreviation();
        } else {
            return getShortestUnique();
        }
    }

    @Override
    public String toString() {
        return String.format("Abbreviation{name=%s, iso=%s, medline=%s, shortestUnique=%s}", getName(), getIsoAbbreviation(), getMedlineAbbreviation(), getShortestUnique());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Abbreviation) {
            Abbreviation that = (Abbreviation) o;
            return Objects.equals(getName(), that.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }
}
