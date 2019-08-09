package org.jabref.gui.texparser;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.texparser.Citation;

public class CitationsDisplay extends ListView<Citation> {

    private ObjectProperty<Path> basePath;

    public CitationsDisplay() {
        this.basePath = new SimpleObjectProperty<>(null);
        new ViewModelListCellFactory<Citation>().withGraphic(this::getDisplayGraphic)
                                                .withTooltip(Citation::getLineText)
                                                .install(this);
    }

    public ObjectProperty<Path> basePathProperty() {
        return basePath;
    }

    public Node getDisplayGraphic(Citation item) {
        if (basePath.get() == null) {
            basePath.set(item.getPath().getRoot());
        }

        Node citationIcon = IconTheme.JabRefIcons.LATEX_COMMENT.getGraphicNode();
        Text contextText = new Text(LatexToUnicodeAdapter.format(item.getContext()));
        contextText.wrappingWidthProperty().bind(this.widthProperty().subtract(85));
        HBox contextBox = new HBox(8, citationIcon, contextText);
        contextBox.setStyle("-fx-border-color: grey;-fx-border-insets: 5;-fx-border-style: dashed;-fx-border-width: 2;-fx-padding: 12;");

        Label fileNameLabel = new Label(String.format("%s", basePath.get().relativize(item.getPath())));
        fileNameLabel.setStyle("-fx-font-family: 'Courier New', Courier, monospace;-fx-font-weight: bold;-fx-label-padding: 5 0 10 10;");
        fileNameLabel.setGraphic(IconTheme.JabRefIcons.LATEX_FILE.getGraphicNode());
        Label positionLabel = new Label(String.format("(%s:%s-%s)", item.getLine(), item.getColStart(), item.getColEnd()));
        positionLabel.setStyle("-fx-font-family: 'Courier New', Courier, monospace;-fx-font-weight: bold;-fx-label-padding: 5 0 10 10;");
        positionLabel.setGraphic(IconTheme.JabRefIcons.LATEX_LINE.getGraphicNode());
        HBox dataBox = new HBox(5, fileNameLabel, positionLabel);

        return new VBox(contextBox, dataBox);
    }
}
