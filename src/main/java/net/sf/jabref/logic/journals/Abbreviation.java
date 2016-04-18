package net.sf.jabref.logic.journals;

import java.util.Objects;

public class Abbreviation implements Comparable<Abbreviation> {

    private final static String SPLITTER = ";"; // elements after SPLITTER are not used at the moment

    private final String name;
    private final String abbreviation;

    public Abbreviation(String name, String abbreviation) {
        this.name = java.util.Objects.requireNonNull(name).trim();
        this.abbreviation = java.util.Objects.requireNonNull(abbreviation).trim();
    }

    public String getName() {
        return name;
    }

    public String getIsoAbbreviation() {
        if (abbreviation.contains(SPLITTER)) {
            String[] restParts = abbreviation.split(SPLITTER);
            return restParts[0].trim();
        }
        return abbreviation;
    }

    public String getMedlineAbbreviation() {
        return getIsoAbbreviation().replace(".", " ").replace("  ", " ").trim();
    }

    @Override
    public int compareTo(Abbreviation toCompare) {
        return name.compareTo(toCompare.name);
    }

    public String getNext(String current) {
        String currentTrimmed = current.trim();

        if (getMedlineAbbreviation().equals(currentTrimmed)) {
            return getName();
        } else if (getName().equals(currentTrimmed)) {
            return getIsoAbbreviation();
        } else {
            return getMedlineAbbreviation();
        }
    }

    @Override
    public String toString() {
        return String.format("Abbreviation{name=%s, iso=%s, medline=%s}", name, getIsoAbbreviation(), getMedlineAbbreviation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Abbreviation) {
            Abbreviation that = (Abbreviation) o;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
