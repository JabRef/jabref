package org.jabref.logic.journals;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class Abbreviation implements Comparable<Abbreviation>, Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();

    private final transient String name;
    private final String abbreviation;
    private final transient String dotlessAbbreviation;
    private String shortestUniqueAbbreviation;

    public Abbreviation(String name, String abbreviation) {
        this(name, abbreviation, "");
    }

    public Abbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
        this(name,
                abbreviation,
                abbreviation.replace(".", " ").replace("  ", " ").trim(),
                shortestUniqueAbbreviation.trim());
    }

    private Abbreviation(String name, String abbreviation, String dotlessAbbreviation,
                         String shortestUniqueAbbreviation) {
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
        if (shortestUniqueAbbreviation.isEmpty()) {
            shortestUniqueAbbreviation = getAbbreviation();
        }
        return shortestUniqueAbbreviation;
    }

    public boolean isDefaultShortestUniqueAbbreviation() {
        return (shortestUniqueAbbreviation.isEmpty()) || this.shortestUniqueAbbreviation.equals(this.abbreviation);
    }

    public String getDotlessAbbreviation() {
        return this.dotlessAbbreviation;
    }

    @Override
    public int compareTo(Abbreviation toCompare) {
        if (isSimilar(toCompare.getName())) {
            return 0;
        }

        int nameComparison = getName().compareTo(toCompare.getName());
        if (nameComparison != 0) {
            return nameComparison;
        }

        if (isSimilar(toCompare.getAbbreviation())) {
            return 0;
        }

        int abbreviationComparison = getAbbreviation().compareTo(toCompare.getAbbreviation());
        if (abbreviationComparison != 0) {
            return abbreviationComparison;
        }

        return getShortestUniqueAbbreviation().compareTo(toCompare.getShortestUniqueAbbreviation());
    }

    public String getNext(String current) {
        String currentTrimmed = current.trim();

        if (getDotlessAbbreviation().equals(currentTrimmed)) {
            return getShortestUniqueAbbreviation().equals(getAbbreviation()) ?
                    getName() : getShortestUniqueAbbreviation();
        } else if (getShortestUniqueAbbreviation().equals(currentTrimmed) &&
                !getShortestUniqueAbbreviation().equals(getAbbreviation())) {
            return getName();
        } else if (getName().equals(currentTrimmed)) {
            return getAbbreviation();
        } else {
            return getDotlessAbbreviation();
        }
    }

    public boolean isSimilar(String otherName) {
        String normalizedThis = normalize(this.name);
        String normalizedOther = normalize(otherName);

        int distance = LEVENSHTEIN.apply(normalizedThis, normalizedOther);
        return distance <= 2;
    }

    private static String normalize(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Abbreviation that = (Abbreviation) o;
        return Objects.equals(normalize(name), normalize(that.name)) &&
                Objects.equals(normalize(abbreviation), normalize(that.abbreviation)) &&
                Objects.equals(normalize(shortestUniqueAbbreviation), normalize(that.shortestUniqueAbbreviation));
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(getName()), normalize(getAbbreviation()), normalize(getShortestUniqueAbbreviation()));
    }

    @Override
    public String toString() {
        return "Abbreviation{name=%s, abbreviation=%s, dotlessAbbreviation=%s, shortestUniqueAbbreviation=%s}".formatted(
                this.name,
                this.abbreviation,
                this.dotlessAbbreviation,
                this.shortestUniqueAbbreviation);
    }
}
