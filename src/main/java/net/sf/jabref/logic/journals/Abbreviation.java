package net.sf.jabref.logic.journals;

import com.google.common.base.Objects;

public class Abbreviation implements Comparable<Abbreviation> {

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
        String SPLITTER = ";"; // elements after SPLITTER are not used at the moment
        if (abbreviation.contains(SPLITTER)) {
            String[] restParts = abbreviation.split(SPLITTER);
            return restParts[0].trim();
        }
        return abbreviation;
    }

    public String getMedlineAbbreviation() {
        return getIsoAbbreviation().replaceAll("\\.", " ").replaceAll("  ", " ").trim();
    }

    public boolean hasIsoAndMedlineAbbreviationsAreSame() {
        return getIsoAbbreviation().equals(getMedlineAbbreviation());
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

    public String toPropertiesLine() {
        return String.format("%s = %s", name, getAbbreviation());
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        Abbreviation that = (Abbreviation) o;
        return Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
