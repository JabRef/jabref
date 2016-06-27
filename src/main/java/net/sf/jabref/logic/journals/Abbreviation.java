/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.journals;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;

public class Abbreviation implements Comparable<Abbreviation> {

    private static final String SPLITTER = ";"; // elements after SPLITTER are not used at the moment

    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty abbreviation = new SimpleStringProperty("");

    public Abbreviation(String name, String abbreviation) {
        this.name.set(java.util.Objects.requireNonNull(name).trim());
        this.abbreviation.set(java.util.Objects.requireNonNull(abbreviation).trim());
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
        return String.format("Abbreviation{name=%s, iso=%s, medline=%s}", getName(), getIsoAbbreviation(), getMedlineAbbreviation());
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
