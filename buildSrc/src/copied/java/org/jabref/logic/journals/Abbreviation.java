package org.jabref.logic.journals;

import java.io.Serializable;
import java.util.Objects;

public class Abbreviation implements Comparable<Abbreviation>, Serializable {

    private static final long serialVersionUID = 1;

    private transient String name;
    private final String abbreviation;
    private transient String dotlessAbbreviation;
    private final String shortestUniqueAbbreviation;

    public Abbreviation(String name, String abbreviation) {
        this(name, abbreviation, "");
    }

    public Abbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        this(name,
                abbreviation,
                // "L. N." becomes "L  N ", we need to remove the double spaces inbetween
                abbreviation.replace(".", " ").replace("  ", " ").trim(),
                shortestUniqueAbbreviation.trim());
    }

    public Abbreviation(String name, String abbreviation, String dotlessAbbreviation, String shortestUniqueAbbreviation) {
        this.name = name.intern();
        this.abbreviation = abbreviation.intern();
        this.dotlessAbbreviation = dotlessAbbreviation.intern();
        this.shortestUniqueAbbreviation = shortestUniqueAbbreviation.trim().intern();
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

    public String getDotlessAbbreviation() {
        return this.dotlessAbbreviation;
    }

    @Override
    public int compareTo(Abbreviation toCompare) {
        return getName().compareTo(toCompare.getName());
    }

    public String getNext(String current) {
        String currentTrimmed = current.trim();

        if (getDotlessAbbreviation().equals(currentTrimmed)) {
            return getShortestUniqueAbbreviation().equals(getAbbreviation()) ? getName() : getShortestUniqueAbbreviation();
        } else if (getShortestUniqueAbbreviation().equals(currentTrimmed) && !getShortestUniqueAbbreviation().equals(getAbbreviation())) {
            return getName();
        } else if (getName().equals(currentTrimmed)) {
            return getAbbreviation();
        } else {
            return getDotlessAbbreviation();
        }
    }

    @Override
    public String toString() {
        return String.format("Abbreviation{name=%s, abbreviation=%s, dotlessAbbreviation=%s, shortestUniqueAbbreviation=%s}",
                this.name,
                this.abbreviation,
                this.dotlessAbbreviation,
                this.shortestUniqueAbbreviation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
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
