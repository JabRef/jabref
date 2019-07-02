package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.Path;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.texparser.jump.JumpToTeXstudio;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseTexResultViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseTexResultViewModel.class);

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

    // TODO: Choose the user editor for pushing entries.
    public void jumpToFile(Path file, int line, int column) {
        try {
            new JumpToTeXstudio().run(file, line, column);
        } catch (IOException e) {
            LOGGER.error("Problem opening the file for jumping.", e);
        }
    }
}
