package org.jabref.gui.texparser;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.texparser.Citation;

public class CitationViewModel {

    private final Citation citation;

    public CitationViewModel(Citation citation) {
        this.citation = citation;
    }

    public Path getPath() {
        return citation.getPath();
    }

    public int getLine() {
        return citation.getLine();
    }

    public int getColStart() {
        return citation.getColStart();
    }

    public int getColEnd() {
        return citation.getColEnd();
    }

    public String getContext() {
        return citation.getContext();
    }

    public Node getDisplayGraphic(Path basePath, Optional<Double> wrappingWidth) {
        Text contextText = new Text(LatexToUnicodeAdapter.format(getContext()));
        wrappingWidth.ifPresent(contextText::setWrappingWidth);

        HBox contextBox = new HBox(contextText);
        contextBox.setStyle("-fx-border-color: grey;-fx-border-insets: 10 0;-fx-border-style: dashed;-fx-border-width: 2;-fx-font-size: 110%;-fx-padding: 10;-fx-pref-width: 500");

        Text positionText = new Text(String.format("%s (%s:%s-%s)", basePath.relativize(getPath()), getLine(), getColStart(), getColEnd()));
        positionText.setStyle("-fx-font-family: monospace;");

        return new VBox(contextBox, positionText);
    }

    @Override
    public String toString() {
        return String.format("CitationViewModel{citation=%s}", this.citation);
    }
}
