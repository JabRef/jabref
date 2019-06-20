package org.jabref.gui.texparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.texparser.Citation;

public class ReferenceWrapper {
    private final String entry;
    private final int count;
    private final ObservableList<Citation> citationList;

    public ReferenceWrapper(String entry, Collection<Citation> citationColl) {
        this.entry = entry;
        this.count = citationColl.size();
        this.citationList = FXCollections.observableList(
                citationColl instanceof List ? (List) citationColl : new ArrayList(citationColl));
    }

    public String getEntry() {
        return entry;
    }

    public int getCount() {
        return count;
    }

    public ObservableList<Citation> citationListProperty() {
        return citationList;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", entry, count);
    }
}
