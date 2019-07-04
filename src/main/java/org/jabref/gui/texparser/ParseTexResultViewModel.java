package org.jabref.gui.texparser;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;

public class ParseTexResultViewModel extends AbstractViewModel {

    private final ObservableList<ReferenceWrapper> referenceList;
    private final ObservableList<Citation> citationList;

    public ParseTexResultViewModel(TexParserResult texParserResult) {
        this.referenceList = FXCollections.observableArrayList();
        this.citationList = FXCollections.observableArrayList();

        texParserResult.getCitationsKeySet().forEach(key -> referenceList.add(
                new ReferenceWrapper(key, texParserResult.getCitationsByKey(key))));
    }

    public ObservableList<ReferenceWrapper> getReferenceList() {
        return new ReadOnlyListWrapper<>(referenceList);
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }
}
