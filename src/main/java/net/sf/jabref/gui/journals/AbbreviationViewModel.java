/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.gui.journals;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import net.sf.jabref.logic.journals.Abbreviation;

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

    public void setName(String name) {
        this.name.set(name);
    }

    public void setAbbreviation(String name) {
        this.abbreviation.set(name);
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
}
