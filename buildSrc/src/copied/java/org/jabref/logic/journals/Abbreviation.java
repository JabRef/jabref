package org.jabref.logic.journals;

import java.util.Objects;

public class Abbreviation implements Comparable<Abbreviation> {

    private final String name;
    private final String abbreviation;
    private final String shortestUniqueAbbreviation;

    public Abbreviation(String name, String abbreviation) {
        this(name, abbreviation, "");
    }

    public Abbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.shortestUniqueAbbreviation = shortestUniqueAbbreviation.trim();
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getShortestUniqueAbbreviation() {
        String result = shortestUniqueAbbreviation;
        if (result.isEmpty()) {
            return getAbbreviation();
        }
        return result;
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
