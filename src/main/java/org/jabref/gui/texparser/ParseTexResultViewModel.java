package org.jabref.gui.texparser;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;

class ParseTexResultViewModel extends AbstractViewModel {

    private final ObservableList<ReferenceViewModel> referenceList;
    private final ObservableList<Citation> citationList;

    public ParseTexResultViewModel(TexParserResult texParserResult) {
        this.referenceList = FXCollections.observableArrayList();
        this.citationList = FXCollections.observableArrayList();

        texParserResult.getCitations()
                       .asMap()
                       .forEach((entry, citations) -> referenceList.add(new ReferenceViewModel(entry, citations)));
    }

    public ObservableList<ReferenceViewModel> getReferenceList() {
        return new ReadOnlyListWrapper<>(referenceList);
    }

    public ObservableList<Citation> getCitationListByReference() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    /**
     * Update the citation list depending on the selected reference.
     */
    public void activeReferenceChanged(ReferenceViewModel reference) {
        if (reference == null) {
            citationList.clear();
        } else {
            citationList.setAll(reference.getCitationList());
        }
    }
}
