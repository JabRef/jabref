package org.jabref.gui.texparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.texparser.Citation;

public class ReferenceWrapper {

    private final String entry;
    private final ObservableList<Citation> citationList;

    public ReferenceWrapper(String entry, Collection<Citation> citationColl) {
        this.entry = entry;
        this.citationList = FXCollections.observableList(
                citationColl instanceof List ? (List) citationColl : new ArrayList(citationColl));
    }

    public String getEntry() {
        return entry;
    }

    public ObservableList<Citation> getCitationList() {
        return citationList;
    }

    public String getDisplayText() {
        return String.format("%s (%s)", entry, citationList.size());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReferenceWrapper.class.getSimpleName() + "[", "]")
                .add("entry='" + entry + "'")
                .add("citationList=" + citationList)
                .toString();
    }
}
