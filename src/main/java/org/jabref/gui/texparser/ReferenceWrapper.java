package org.jabref.gui.texparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.texparser.Citation;

public class ReferenceWrapper {
    private final String entry;
    private final int count;
    private final ObservableList<Citation> citationList;

    public ReferenceWrapper(String entry, Set<Citation> citationSet) {
        this(entry, new ArrayList<>(citationSet));
    }

    public ReferenceWrapper(String entry, List<Citation> citationList) {
        this.entry = entry;
        this.count = citationList.size();
        this.citationList = FXCollections.observableList(citationList);
    }

    public final String getEntry() {
        return entry;
    }

    public final int getCount() {
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
