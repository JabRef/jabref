package org.jabref.gui.texparser;

import java.util.Collection;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.texparser.Citation;

public class ReferenceViewModel {

    private final String entry;
    private final ObservableList<Citation> citationList;

    public ReferenceViewModel(String entry, Collection<Citation> citationColl) {
        this.entry = entry;
        this.citationList = FXCollections.observableArrayList();

        citationList.setAll(citationColl);
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    /**
     * Return a string for displaying an entry key and its number of uses.
     */
    public String getDisplayText() {
        return String.format("%s (%s)", entry, citationList.size());
    }

    @Override
    public String toString() {
        return String.format("ReferenceViewModel{entry='%s', citationList=%s}",
                this.entry,
                this.citationList);
    }
}
