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

public class ParseTexResultViewModel extends AbstractViewModel {

    private final TexParserResult texParserResult;
    private final ObservableList<ReferenceWrapper> referenceList;
    private final ObservableList<Citation> citationList;

    public ParseTexResultViewModel(TexParserResult texParserResult) {
        this.texParserResult = texParserResult;
        this.referenceList = setupReferenceList();
        this.citationList = FXCollections.observableArrayList();
    }

    private ObservableList<ReferenceWrapper> setupReferenceList() {
        ObservableList<ReferenceWrapper> referenceList = FXCollections.observableArrayList();

        for (String key : texParserResult.getCitationsKeySet()) {
            referenceList.add(new ReferenceWrapper(key, texParserResult.getCitationsByKey(key)));
        }

        return referenceList.sorted();
    }

    public ObservableList<ReferenceWrapper> getReferenceList() {
        return new ReadOnlyListWrapper<>(referenceList);
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    // TODO: Add an applications manager for choosing the text editor
    public void jumpToFile(Path file, int line, int column) {
        try {
            new JumpToTeXstudio().run(file, line, column);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
